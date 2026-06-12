package fr.korol;

import fr.korol.model.*;

import java.util.HashSet;
import java.util.Set;

public class MermaidGenerator {

    public String generate(XsdModel model) {
        StringBuilder sb = new StringBuilder("graph LR\n");
        Set<String> processedNodes = new HashSet<>();

        for (XsdElement rootElement : model.getRootElements()) {
            processElement(rootElement, null, sb, processedNodes, model);
        }

        if (sb.length() <= 9) {
            sb.append("  NoElementsFound(\"No top-level elements found\")\n");
        }

        return sb.toString();
    }

    private void processElement(XsdElement element, String parentId, StringBuilder sb, Set<String> processedNodes, XsdModel model) {
        String name = element.getName();
        String ref = element.getRef();

        String displayName = (name != null && !name.isEmpty()) ? name : (ref != null && !ref.isEmpty() ? stripNamespace(ref) : "unknown");
        String elementId = cleanId((parentId != null ? parentId : "") + "_" + displayName);

        if (ref == null || ref.isEmpty()) {
            sb.append("  ").append(elementId).append("(\"").append(displayName).append("\")\n");
            if (parentId != null) {
                sb.append("  ").append(parentId).append(" --> ").append(elementId).append("\n");
            }
        } else {
            String refName = stripNamespace(ref);
            String refId = cleanId(refName);
            sb.append("  ").append(refId).append("(\"").append(refName).append("\")\n");
            if (parentId != null) {
                sb.append("  ").append(parentId).append(" --> ").append(refId).append("\n");
            }
            elementId = refId;
        }

        String type = element.getType();
        if (type != null && !type.isEmpty()) {
            if (model.getComplexTypes().containsKey(type)) {
                processComplexType(model.getComplexTypes().get(type), elementId, sb, processedNodes, model);
            } else if (model.getSimpleTypes().containsKey(type)) {
                sb.append("  style ").append(elementId).append(" fill:#eef,stroke:#33f,stroke-width:1px\n");
            }
        } else if (element.getAnonymousType() != null) {
            processComplexType(element.getAnonymousType(), elementId, sb, processedNodes, model);
        }
    }

    private void processComplexType(XsdComplexType complexType, String parentId, StringBuilder sb, Set<String> processedNodes, XsdModel model) {
        String ctName = complexType.getName();
        String processingKey = parentId + ":" + (ctName.isEmpty() ? "anonymous" : ctName);
        if (processedNodes.contains(processingKey)) {
            return;
        }
        processedNodes.add(processingKey);

        String baseType = complexType.getBaseType();
        if (baseType != null && !baseType.isEmpty() && model.getComplexTypes().containsKey(baseType)) {
            processComplexType(model.getComplexTypes().get(baseType), parentId, sb, processedNodes, model);
        }

        for (XsdElement nested : complexType.getElements()) {
            processElement(nested, parentId, sb, processedNodes, model);
        }

        for (XsdAttribute attr : complexType.getAttributes()) {
            String attrName = attr.getName();
            String attrId = cleanId(parentId + "_attr_" + attrName);
            sb.append("  ").append(attrId).append("(\"@").append(attrName).append("\")\n");
            sb.append("  ").append(parentId).append(" --> ").append(attrId).append("\n");
            sb.append("  style ").append(attrId).append(" fill:#eee,stroke:#999,stroke-dasharray: 5 5\n");
        }
    }

    private String cleanId(String id) {
        return id.replaceAll("[^a-zA-Z0-9]", "_");
    }

    private String stripNamespace(String value) {
        if (value == null) return "";
        return value.contains(":") ? value.substring(value.indexOf(':') + 1) : value;
    }
}
