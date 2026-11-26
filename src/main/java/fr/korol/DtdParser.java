package fr.korol;

import com.intellij.openapi.vfs.VirtualFile;
import fr.korol.model.*;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.ext.DefaultHandler2;

import javax.xml.parsers.SAXParserFactory;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a DTD into the same {@link XsdModel} as {@link XsdParser}, so the tree, search and dialogs work unchanged.
 * The JDK SAX parser does the DTD work (parameter entities, external subsets); only content models are parsed here.
 */
public class DtdParser {
    private static final String MAIN_DTD = "urn:xsdviewer:main.dtd";
    private static final Pattern TOKEN = Pattern.compile("[^\\s(),|?*+]+|[(),|?*+]");

    private final List<String> rootNames = new ArrayList<>();

    public XsdModel parse(VirtualFile file) throws Exception {
        String baseUri = file.isInLocalFileSystem() ? Path.of(file.getPath()).toUri().toString() : file.getUrl();
        Handler handler = new Handler(file, baseUri);

        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        XMLReader reader = factory.newSAXParser().getXMLReader();
        reader.setProperty("http://xml.org/sax/properties/declaration-handler", handler);
        reader.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
        reader.setEntityResolver(handler);
        reader.parse(new InputSource(new StringReader("<!DOCTYPE dtd SYSTEM \"" + MAIN_DTD + "\"><dtd/>")));

        XsdModel model = new XsdModel();
        Set<String> referenced = new HashSet<>();
        handler.elements.forEach((name, contentModel) -> {
            XsdComplexType type = new XsdComplexType(name);
            type.setDocumentation(handler.docs.get(name));
            parseContentModel(contentModel).forEach((child, occurs) -> {
                referenced.add(child);
                type.addElement(new XsdElement(child, null, child, String.valueOf(occurs[0]), occurs[1] < 0 ? "unbounded" : String.valueOf(occurs[1])));
            });
            handler.attributes.getOrDefault(name, List.of()).forEach(type::addAttribute);
            boolean leaf = type.getElements().isEmpty() && type.getAttributes().isEmpty();
            if (!leaf) model.addComplexType(type);

            // Leaves show their content model ((#PCDATA), EMPTY, ANY) as type
            XsdElement element = new XsdElement(name, leaf ? contentModel : name, null, null, null);
            element.setDocumentation(handler.docs.get(name));
            model.addGlobalElement(element);
        });
        handler.enumerations.values().forEach(model::addSimpleType);

        handler.elements.keySet().stream().filter(name -> !referenced.contains(name)).forEach(rootNames::add);
        if (rootNames.isEmpty() && !handler.elements.isEmpty()) {
            rootNames.add(handler.elements.keySet().iterator().next());
        }
        return model;
    }

    /** Elements no other element references, in declaration order (first declared if every element is referenced). */
    public List<String> getRootNames() {
        return rootNames;
    }

    /**
     * Child element name to {minOccurs, maxOccurs} (maxOccurs -1 = unbounded) for a content model like
     * {@code (a, (b | c)*, d?)}. EMPTY and ANY have no children.
     */
    static Map<String, int[]> parseContentModel(String contentModel) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(contentModel);
        while (matcher.find()) tokens.add(matcher.group());
        if (tokens.isEmpty() || !tokens.get(0).equals("(")) return new LinkedHashMap<>();
        return parseParticle(tokens, new int[]{0});
    }

    private static Map<String, int[]> parseParticle(List<String> tokens, int[] pos) {
        Map<String, int[]> result = new LinkedHashMap<>();
        String token = tokens.get(pos[0]++);
        if (token.equals("(")) {
            boolean choice = false;
            while (true) {
                parseParticle(tokens, pos).forEach((name, occurs) -> result.merge(name, occurs, DtdParser::sum));
                String separator = tokens.get(pos[0]++);
                if (separator.equals(")")) break;
                choice = separator.equals("|");
            }
            // ponytail: a choice makes every branch optional; exact for distinct branches, loose if a name repeats
            if (choice) result.values().forEach(occurs -> occurs[0] = 0);
        } else if (!token.equals("#PCDATA")) {
            result.put(token, new int[]{1, 1});
        }

        int[] factor = pos[0] < tokens.size() ? switch (tokens.get(pos[0])) {
            case "?" -> new int[]{0, 1};
            case "*" -> new int[]{0, -1};
            case "+" -> new int[]{1, -1};
            default -> null;
        } : null;
        if (factor != null) {
            pos[0]++;
            result.values().forEach(occurs -> {
                occurs[0] *= factor[0];
                occurs[1] = occurs[1] < 0 || factor[1] < 0 ? -1 : occurs[1] * factor[1];
            });
        }
        return result;
    }

    private static int[] sum(int[] a, int[] b) {
        return new int[]{a[0] + b[0], a[1] < 0 || b[1] < 0 ? -1 : a[1] + b[1]};
    }

    private static class Handler extends DefaultHandler2 implements DeclHandler {
        private final VirtualFile file;
        private final String baseUri;
        final Map<String, String> elements = new LinkedHashMap<>();
        final Map<String, List<XsdAttribute>> attributes = new HashMap<>();
        final Map<String, XsdSimpleType> enumerations = new HashMap<>();
        final Map<String, String> docs = new HashMap<>();
        private String pendingComment;

        Handler(VirtualFile file, String baseUri) {
            this.file = file;
            this.baseUri = baseUri;
        }

        @Override
        public InputSource resolveEntity(String name, String publicId, String baseURI, String systemId) throws java.io.IOException {
            if (MAIN_DTD.equals(systemId)) {
                InputSource source = new InputSource(file.getInputStream());
                source.setSystemId(baseUri);
                return source;
            }
            String resolved;
            try {
                resolved = baseURI == null ? systemId : java.net.URI.create(baseURI).resolve(systemId).toString();
            } catch (IllegalArgumentException e) { // unescaped relative path (spaces, backslashes): local by construction
                return null;
            }
            // Never touch the network: only local files are loaded, anything else is treated as empty
            return resolved.startsWith("file:") ? null : new InputSource(new StringReader(""));
        }

        @Override
        public void comment(char[] ch, int start, int length) {
            pendingComment = new String(ch, start, length).trim();
        }

        @Override
        public void elementDecl(String name, String model) {
            elements.put(name, model);
            if (pendingComment != null) docs.put(name, pendingComment);
            pendingComment = null;
        }

        @Override
        public void attributeDecl(String eName, String aName, String type, String mode, String value) {
            XsdAttribute attribute = new XsdAttribute(aName, type, "#REQUIRED".equals(mode));
            attribute.setDocumentation(pendingComment);
            pendingComment = null;
            attributes.computeIfAbsent(eName, k -> new ArrayList<>()).add(attribute);

            if (type.startsWith("(")) {
                XsdSimpleType simpleType = new XsdSimpleType(type);
                for (String v : type.substring(1, type.length() - 1).split("\\|")) {
                    simpleType.addEnumeration(v.trim(), null);
                }
                enumerations.put(type, simpleType);
            }
        }

        @Override
        public void internalEntityDecl(String name, String value) {
            pendingComment = null;
        }

        @Override
        public void externalEntityDecl(String name, String publicId, String systemId) {
            pendingComment = null;
        }
    }
}
