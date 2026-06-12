package fr.korol;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.jcef.JBCefBrowser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.beans.PropertyChangeListener;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class XsdFileEditor extends UserDataHolderBase implements FileEditor {
    private final Project project;
    private final VirtualFile file;
    private final JBCefBrowser browser;

    public XsdFileEditor(@NotNull Project project, @NotNull VirtualFile file) {
        this.project = project;
        this.file = file;
        this.browser = new JBCefBrowser();
        loadGraph();
    }

    private void loadGraph() {
        try {
            String xsdContent = new String(file.contentsToByteArray());
            String mermaidData = parseXsdToMermaid(xsdContent);
            String html = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "    <script src=\"https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js\"></script>\n" +
                    "    <script>\n" +
                    "        mermaid.initialize({ \n" +
                    "            startOnLoad: true,\n" +
                    "            flowchart: {\n" +
                    "                useMaxWidth: false,\n" +
                    "                htmlLabels: true\n" +
                    "            }\n" +
                    "        });\n" +
                    "    </script>\n" +
                    "    <style>\n" +
                    "        body { background-color: white; margin: 20px; }\n" +
                    "        .mermaid { overflow: auto; }\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <div class=\"mermaid\">\n" +
                    "        " + mermaidData + "\n" +
                    "    </div>\n" +
                    "</body>\n" +
                    "</html>";
            browser.loadHTML(html);
        } catch (Exception e) {
            browser.loadHTML("Error reading file: " + e.getMessage());
        }
    }

    private String parseXsdToMermaid(String content) {
        StringBuilder sb = new StringBuilder("graph LR\n");
        Set<String> processedNodes = new HashSet<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(content)));

            Map<String, Element> complexTypeMap = new HashMap<>();
            Map<String, Element> simpleTypeMap = new HashMap<>();

            NodeList complexTypes = doc.getElementsByTagNameNS("*", "complexType");
            for (int i = 0; i < complexTypes.getLength(); i++) {
                Element ct = (Element) complexTypes.item(i);
                String name = ct.getAttribute("name");
                if (!name.isEmpty()) complexTypeMap.put(name, ct);
            }

            NodeList simpleTypes = doc.getElementsByTagNameNS("*", "simpleType");
            for (int i = 0; i < simpleTypes.getLength(); i++) {
                Element st = (Element) simpleTypes.item(i);
                String name = st.getAttribute("name");
                if (!name.isEmpty()) simpleTypeMap.put(name, st);
            }

            ParserContext context = new ParserContext(sb, processedNodes, complexTypeMap, simpleTypeMap);

            NodeList rootElements = doc.getDocumentElement().getChildNodes();
            for (int i = 0; i < rootElements.getLength(); i++) {
                Node node = rootElements.item(i);
                if (node instanceof Element element && "element".equals(element.getLocalName())) {
                    processElement(element, null, context);
                }
            }

        } catch (Exception e) {
            sb.append("  Error(\"Parsing Error: ").append(e.getMessage() != null ? (e.getMessage().length() > 50 ? e.getMessage().substring(0, 50) : e.getMessage()) : "unknown").append("\")\n");
        }

        if (sb.length() <= 9) {
            sb.append("  NoElementsFound(\"No top-level elements found\")\n");
        }

        return sb.toString();
    }

    private static class ParserContext {
        final StringBuilder sb;
        final Set<String> processedNodes;
        final Map<String, Element> complexTypeMap;
        final Map<String, Element> simpleTypeMap;

        ParserContext(StringBuilder sb, Set<String> processedNodes, Map<String, Element> complexTypeMap, Map<String, Element> simpleTypeMap) {
            this.sb = sb;
            this.processedNodes = processedNodes;
            this.complexTypeMap = complexTypeMap;
            this.simpleTypeMap = simpleTypeMap;
        }
    }

    private String cleanId(String id) {
        return id.replaceAll("[^a-zA-Z0-9]", "_");
    }

    private void processElement(Element element, String parentId, ParserContext context) {
        String name = element.getAttribute("name");
        String ref = element.getAttribute("ref");

        String displayName = !name.isEmpty() ? name : (!ref.isEmpty() ? ref.substring(ref.indexOf(':') + 1) : "unknown");
        String elementId = cleanId((parentId != null ? parentId : "") + "_" + displayName);

        if (ref.isEmpty()) {
            context.sb.append("  ").append(elementId).append("(\"").append(displayName).append("\")\n");
            if (parentId != null) {
                context.sb.append("  ").append(parentId).append(" --> ").append(elementId).append("\n");
            }
        } else {
            // If it's a ref, we want to use the parent's ID to point to the referenced element's ID
            String refName = ref.contains(":") ? ref.substring(ref.indexOf(':') + 1) : ref;
            String refId = cleanId(refName);
            context.sb.append("  ").append(refId).append("(\"").append(refName).append("\")\n");
            if (parentId != null) {
                context.sb.append("  ").append(parentId).append(" --> ").append(refId).append("\n");
            }
            // Update elementId so that nested content (if any, though rare for ref) points from refId
            elementId = refId;
        }

        String typeAttr = element.getAttribute("type");
        String type = typeAttr.contains(":") ? typeAttr.substring(typeAttr.indexOf(':') + 1) : typeAttr;
        
        if (!type.isEmpty()) {
            if (context.complexTypeMap.containsKey(type)) {
                processComplexType(context.complexTypeMap.get(type), elementId, context);
            } else if (context.simpleTypeMap.containsKey(type)) {
                context.sb.append("  style ").append(elementId).append(" fill:#eef,stroke:#33f,stroke-width:1px\n");
            }
        } else if (ref.isEmpty()) {
            NodeList ctNodes = element.getElementsByTagNameNS("*", "complexType");
            if (ctNodes.getLength() > 0) {
                processComplexType((Element) ctNodes.item(0), elementId, context);
            } else {
                NodeList stNodes = element.getElementsByTagNameNS("*", "simpleType");
                if (stNodes.getLength() > 0) {
                    context.sb.append("  style ").append(elementId).append(" fill:#eef,stroke:#33f\n");
                }
            }
        }
    }

    private void processComplexType(Element ctElement, String parentId, ParserContext context) {
        String ctName = ctElement.getAttribute("name");
        String processingKey = parentId + ":" + (ctName.isEmpty() ? "anonymous" : ctName);
        if (context.processedNodes.contains(processingKey)) {
            return;
        }
        context.processedNodes.add(processingKey);

        NodeList extensions = ctElement.getElementsByTagNameNS("*", "extension");
        for (int i = 0; i < extensions.getLength(); i++) {
            Element extension = (Element) extensions.item(i);
            String baseAttr = extension.getAttribute("base");
            String base = baseAttr.contains(":") ? baseAttr.substring(baseAttr.indexOf(':') + 1) : baseAttr;
            if (!base.isEmpty() && context.complexTypeMap.containsKey(base)) {
                processComplexType(context.complexTypeMap.get(base), parentId, context);
            }
        }

        NodeList restrictions = ctElement.getElementsByTagNameNS("*", "restriction");
        for (int i = 0; i < restrictions.getLength(); i++) {
            Element restriction = (Element) restrictions.item(i);
            String baseAttr = restriction.getAttribute("base");
            String base = baseAttr.contains(":") ? baseAttr.substring(baseAttr.indexOf(':') + 1) : baseAttr;
            if (!base.isEmpty() && context.complexTypeMap.containsKey(base)) {
                processComplexType(context.complexTypeMap.get(base), parentId, context);
            }
        }

        NodeList nestedElements = ctElement.getElementsByTagNameNS("*", "element");
        for (int i = 0; i < nestedElements.getLength(); i++) {
            Element nested = (Element) nestedElements.item(i);
            Node p = nested.getParentNode();
            while (p != null && p != ctElement) {
                if (p instanceof Element pe && ("element".equals(pe.getLocalName()) || "complexType".equals(pe.getLocalName()))) break;
                p = p.getParentNode();
            }
            if (p == ctElement) {
                processElement(nested, parentId, context);
            }
        }

        NodeList attributes = ctElement.getElementsByTagNameNS("*", "attribute");
        for (int i = 0; i < attributes.getLength(); i++) {
            Element attr = (Element) attributes.item(i);
            String attrName = attr.getAttribute("name");
            if (!attrName.isEmpty()) {
                String attrId = cleanId(parentId + "_attr_" + attrName);
                context.sb.append("  ").append(attrId).append("(\"@").append(attrName).append("\")\n");
                context.sb.append("  ").append(parentId).append(" --> ").append(attrId).append("\n");
                context.sb.append("  style ").append(attrId).append(" fill:#eee,stroke:#999,stroke-dasharray: 5 5\n");
            }
        }
    }

    @Override
    public @NotNull JComponent getComponent() {
        return browser.getComponent();
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return browser.getComponent();
    }

    @Override
    public @NotNull String getName() {
        return "XSD Graph";
    }

    @Override
    public void setState(@NotNull FileEditorState state) {}

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {}

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {}

    @Override
    public @Nullable FileEditorLocation getCurrentLocation() {
        return null;
    }

    @Override
    public void dispose() {
        browser.dispose();
    }
}
