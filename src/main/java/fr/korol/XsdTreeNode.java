package fr.korol;

import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import fr.korol.model.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.HashSet;
import java.util.Set;

public class XsdTreeNode extends DefaultMutableTreeNode {
    private final String displayName;
    private final String type;
    private final boolean isAttribute;
    private final String minOccurs;
    private final String maxOccurs;
    private final boolean required;
    private final String documentation;

    public XsdTreeNode(String displayName, String type, boolean isAttribute, String minOccurs, String maxOccurs, boolean required, String documentation) {
        this.displayName = displayName;
        this.type = type;
        this.isAttribute = isAttribute;
        this.minOccurs = minOccurs;
        this.maxOccurs = maxOccurs;
        this.required = required;
        this.documentation = documentation;
    }

    public static XsdTreeNode createTree(XsdModel model, String rootElementName) {
        XsdElement rootElement = model.getGlobalElements().get(rootElementName);
        if (rootElement == null && !model.getGlobalElements().isEmpty()) {
            rootElement = model.getGlobalElements().values().iterator().next();
        }

        if (rootElement == null) {
            return new XsdTreeNode(MyMessageBundle.message("editor.noroot"), null, false, null, null, false, null);
        }

        return processElement(rootElement, model, 0, new HashSet<>());
    }

    private static XsdTreeNode processElement(XsdElement element, XsdModel model, int depth, Set<String> visitedTypes) {
        if (depth > 15) {
            return new XsdTreeNode(MyMessageBundle.message("editor.depth",depth), null, false, null, null, false, null);
        }

        String name = element.getName();
        String ref = element.getRef();
        String type = element.getType();
        String minOccurs = element.getMinOccurs();
        String maxOccurs = element.getMaxOccurs();
        XsdComplexType anonymousType = element.getAnonymousType();

        String displayName = (name != null && !name.isEmpty()) ? name : (ref != null && !ref.isEmpty() ? stripNamespace(ref) : "unknown");
        String documentation = element.getDocumentation();

        if (ref != null && !ref.isEmpty()) {
            String refName = stripNamespace(ref);
            XsdElement globalEl = model.getGlobalElements().get(refName);
            if (globalEl != null) {
                if (globalEl.getType() != null && !globalEl.getType().isEmpty()) {
                    type = globalEl.getType();
                } else if (globalEl.getAnonymousType() != null) {
                    anonymousType = globalEl.getAnonymousType();
                }
                if (documentation == null || documentation.isEmpty()) {
                    documentation = globalEl.getDocumentation();
                }
            }
        }

        if (documentation == null || documentation.isEmpty()) {
            if (type != null && model.getComplexTypes().containsKey(type)) {
                documentation = model.getComplexTypes().get(type).getDocumentation();
            } else if (type != null && model.getSimpleTypes().containsKey(type)) {
                documentation = model.getSimpleTypes().get(type).getDocumentation();
            }
        }

        XsdTreeNode node = new XsdTreeNode(displayName, type, false, minOccurs, maxOccurs, false, documentation);

        if (type != null && !type.isEmpty()) {
            if (model.getComplexTypes().containsKey(type)) {
                if (!visitedTypes.contains(type)) {
                    visitedTypes.add(type);
                    addComplexTypeChildren(node, model.getComplexTypes().get(type), model, depth + 1, visitedTypes);
                    visitedTypes.remove(type);
                } else {
                    node.add(new XsdTreeNode(MyMessageBundle.message("editor.cyclic",type), null, false, null, null, false, null));
                }
            }
        } else if (anonymousType != null) {
            addComplexTypeChildren(node, anonymousType, model, depth + 1, visitedTypes);
        }

        return node;
    }

    private static void addComplexTypeChildren(XsdTreeNode node, XsdComplexType complexType, XsdModel model, int depth, Set<String> visitedTypes) {
        String baseType = complexType.getBaseType();
        if (baseType != null && !baseType.isEmpty() && model.getComplexTypes().containsKey(baseType)) {
            if (!visitedTypes.contains(baseType)) {
                visitedTypes.add(baseType);
                addComplexTypeChildren(node, model.getComplexTypes().get(baseType), model, depth, visitedTypes);
                visitedTypes.remove(baseType);
            }
        }

        for (XsdAttribute attr : complexType.getAttributes()) {
            node.add(new XsdTreeNode("@" + attr.getName(), attr.getType(), true, null, null, attr.isRequired(), attr.getDocumentation()));
        }

        for (XsdElement nested : complexType.getElements()) {
            node.add(processElement(nested, model, depth + 1, visitedTypes));
        }
    }

    private static String stripNamespace(String value) {
        if (value == null) return "";
        return value.contains(":") ? value.substring(value.indexOf(':') + 1) : value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getType() {
        return type;
    }

    public boolean isAttribute() {
        return isAttribute;
    }

    public String getMinOccurs() {
        return minOccurs;
    }

    public String getMaxOccurs() {
        return maxOccurs;
    }

    public boolean isRequired() {
        return required;
    }

    public String getDocumentation() {
        return documentation;
    }

    public boolean isMandatory() {
        if (isAttribute) {
            return required;
        }
        // For elements, mandatory if minOccurs is not "0"
        // Default minOccurs is 1 if not specified
        return minOccurs == null || minOccurs.isEmpty() || !"0".equals(minOccurs);
    }

    public static class XsdTreeCellRenderer extends ColoredTreeCellRenderer {
        @Override
        public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            if (value instanceof XsdTreeNode node) {
                SimpleTextAttributes mainAttributes = node.isMandatory()
                        ? SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
                        : SimpleTextAttributes.REGULAR_ATTRIBUTES;

                append(node.getDisplayName(), mainAttributes);

                if (node.getType() != null && !node.getType().isEmpty()) {
                    append(" : ", SimpleTextAttributes.GRAYED_ATTRIBUTES);
                    append(node.getType(), SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
                }

                if (!node.isAttribute()) {
                    String min = (node.getMinOccurs() == null || node.getMinOccurs().isEmpty()) ? "1" : node.getMinOccurs();
                    String max = (node.getMaxOccurs() == null || node.getMaxOccurs().isEmpty()) ? "1" : node.getMaxOccurs();
                    if ("unbounded".equals(max)) {
                        max = "*";
                    }

                    // Only show if it's not the default 1..1 or if user explicitly wants to see it
                    // The issue asks to make visible how many items are allowed (min-max)
                    append(" [" + min + ".." + max + "]", SimpleTextAttributes.GRAYED_ATTRIBUTES);
                } else if (node.isRequired()) {
                    append(" [1..1]", SimpleTextAttributes.GRAYED_ATTRIBUTES);
                } else {
                    append(" [0..1]", SimpleTextAttributes.GRAYED_ATTRIBUTES);
                }
            }
        }
    }
}
