package fr.korol;

import com.intellij.openapi.vfs.VirtualFile;
import fr.korol.model.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

public class XsdParser {
    private final Set<String> processedFiles = new HashSet<>();

    public XsdModel parse(VirtualFile file) throws Exception {
        XsdModel model = new XsdModel();
        processedFiles.clear();
        parseInternal(file, model);
        return model;
    }

    private void parseInternal(VirtualFile file, XsdModel model) throws Exception {
        if (file == null || !processedFiles.add(file.getPath())) {
            return;
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc;
        try (var is = file.getInputStream()) {
            doc = builder.parse(is);
        }

        // Handle imports and includes
        NodeList imports = doc.getElementsByTagNameNS("*", "import");
        for (int i = 0; i < imports.getLength(); i++) {
            Element imp = (Element) imports.item(i);
            String schemaLocation = imp.getAttribute("schemaLocation");
            if (!schemaLocation.isEmpty()) {
                VirtualFile parent = file.getParent();
                if (parent != null) {
                    VirtualFile importedFile = parent.findFileByRelativePath(schemaLocation);
                    if (importedFile != null) {
                        parseInternal(importedFile, model);
                    }
                }
            }
        }

        NodeList includes = doc.getElementsByTagNameNS("*", "include");
        for (int i = 0; i < includes.getLength(); i++) {
            Element inc = (Element) includes.item(i);
            String schemaLocation = inc.getAttribute("schemaLocation");
            if (!schemaLocation.isEmpty()) {
                VirtualFile parent = file.getParent();
                if (parent != null) {
                    VirtualFile includedFile = parent.findFileByRelativePath(schemaLocation);
                    if (includedFile != null) {
                        parseInternal(includedFile, model);
                    }
                }
            }
        }

        NodeList complexTypes = doc.getElementsByTagNameNS("*", "complexType");
        for (int i = 0; i < complexTypes.getLength(); i++) {
            Element ct = (Element) complexTypes.item(i);
            String name = ct.getAttribute("name");
            if (!name.isEmpty()) {
                XsdComplexType complexType = new XsdComplexType(name);
                complexType.setDocumentation(extractDocumentation(ct));
                model.addComplexType(complexType);
            }
        }

        NodeList simpleTypes = doc.getElementsByTagNameNS("*", "simpleType");
        for (int i = 0; i < simpleTypes.getLength(); i++) {
            Element st = (Element) simpleTypes.item(i);
            String name = st.getAttribute("name");
            if (!name.isEmpty()) {
                XsdSimpleType simpleType = new XsdSimpleType(name);
                simpleType.setDocumentation(extractDocumentation(st));
                model.addSimpleType(simpleType);
            }
        }

        // Second pass to fill complex types
        for (int i = 0; i < complexTypes.getLength(); i++) {
            Element ctElement = (Element) complexTypes.item(i);
            String name = ctElement.getAttribute("name");
            if (!name.isEmpty()) {
                XsdComplexType complexType = model.getComplexTypes().get(name);
                fillComplexType(complexType, ctElement, model);
            }
        }

        NodeList rootElements = doc.getDocumentElement().getChildNodes();
        for (int i = 0; i < rootElements.getLength(); i++) {
            Node node = rootElements.item(i);
            if (node instanceof Element element && "element".equals(element.getLocalName())) {
                XsdElement xsdElement = parseElement(element, model);
                model.addGlobalElement(xsdElement);
            }
        }
    }

    private void fillComplexType(XsdComplexType complexType, Element ctElement, XsdModel model) {
        NodeList extensions = ctElement.getElementsByTagNameNS("*", "extension");
        for (int i = 0; i < extensions.getLength(); i++) {
            Element extension = (Element) extensions.item(i);
            String baseAttr = extension.getAttribute("base");
            complexType.setBaseType(stripNamespace(baseAttr));
        }

        NodeList restrictions = ctElement.getElementsByTagNameNS("*", "restriction");
        for (int i = 0; i < restrictions.getLength(); i++) {
            Element restriction = (Element) restrictions.item(i);
            String baseAttr = restriction.getAttribute("base");
            complexType.setBaseType(stripNamespace(baseAttr));
        }

        NodeList nestedElements = ctElement.getElementsByTagNameNS("*", "element");
        for (int i = 0; i < nestedElements.getLength(); i++) {
            Element nested = (Element) nestedElements.item(i);
            if (isDirectChildOf(nested, ctElement)) {
                complexType.addElement(parseElement(nested, model));
            }
        }

        NodeList attributes = ctElement.getElementsByTagNameNS("*", "attribute");
        for (int i = 0; i < attributes.getLength(); i++) {
            Element attr = (Element) attributes.item(i);
            String attrName = attr.getAttribute("name");
            if (!attrName.isEmpty()) {
                String attrType = stripNamespace(attr.getAttribute("type"));
                boolean required = "required".equals(attr.getAttribute("use"));
                XsdAttribute xsdAttribute = new XsdAttribute(attrName, attrType, required);
                xsdAttribute.setDocumentation(extractDocumentation(attr));
                complexType.addAttribute(xsdAttribute);
            }
        }
    }

    private XsdElement parseElement(Element element, XsdModel model) {
        String name = element.getAttribute("name");
        String ref = element.getAttribute("ref");
        String typeAttr = element.getAttribute("type");
        String type = stripNamespace(typeAttr);
        String minOccurs = element.getAttribute("minOccurs");
        String maxOccurs = element.getAttribute("maxOccurs");

        XsdElement xsdElement = new XsdElement(name, type, ref, minOccurs, maxOccurs);
        xsdElement.setDocumentation(extractDocumentation(element));

        NodeList ctNodes = element.getElementsByTagNameNS("*", "complexType");
        if (ctNodes.getLength() > 0) {
            XsdComplexType anonymousType = new XsdComplexType("");
            fillComplexType(anonymousType, (Element) ctNodes.item(0), model);
            xsdElement.setAnonymousType(anonymousType);
        }

        return xsdElement;
    }

    private String stripNamespace(String value) {
        if (value == null) return "";
        return value.contains(":") ? value.substring(value.indexOf(':') + 1) : value;
    }

    private String extractDocumentation(Element element) {
        NodeList annotations = element.getElementsByTagNameNS("*", "annotation");
        if (annotations.getLength() > 0) {
            Element annotation = (Element) annotations.item(0);
            NodeList docs = annotation.getElementsByTagNameNS("*", "documentation");
            if (docs.getLength() > 0) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < docs.getLength(); i++) {
                    Element doc = (Element) docs.item(i);
                    String definition = getChildTagContent(doc, "Definition");
                    String examples = getChildTagContent(doc, "Examples");

                    if (definition != null || examples != null) {
                        if (definition != null) {
                            sb.append(MyMessageBundle.message("tooltip.definition",definition)).append("\n");
                        }
                        if (examples != null) {
                            sb.append(MyMessageBundle.message("tooltip.examples",examples)).append("\n");
                        }
                    } else {
                        sb.append(doc.getTextContent().trim()).append("\n");
                    }
                }
                return sb.toString().trim();
            }
        }
        return null;
    }

    private String getChildTagContent(Element element, String tagName) {
        NodeList list = element.getElementsByTagNameNS("*", tagName);
        if (list.getLength() > 0) {
            return list.item(0).getTextContent().trim();
        }
        return null;
    }

    private boolean isDirectChildOf(Element element, Element parent) {
        Node p = element.getParentNode();
        while (p != null && p != parent) {
            if (p instanceof Element pe && ("element".equals(pe.getLocalName()) || "complexType".equals(pe.getLocalName()))) {
                return false;
            }
            p = p.getParentNode();
        }
        return p == parent;
    }
}
