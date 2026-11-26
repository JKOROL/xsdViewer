package fr.korol;

import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import fr.korol.model.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class XsdTreeNode extends DefaultMutableTreeNode {
    private final String displayName;
    private final String fullDisplayName;
    private final String type;
    private final String fullType;
    private final boolean isAttribute;
    private final String minOccurs;
    private final String maxOccurs;
    private final boolean required;
    private final String documentation;

    public XsdTreeNode(String displayName, String fullDisplayName, String type, String fullType, boolean isAttribute, String minOccurs, String maxOccurs, boolean required, String documentation) {
        this.displayName = displayName;
        this.fullDisplayName = fullDisplayName;
        this.type = type;
        this.fullType = fullType;
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
            return new XsdTreeNode(MyMessageBundle.message("editor.noroot"), MyMessageBundle.message("editor.noroot"), null, null, false, null, null, false, null);
        }

        Builder builder = new Builder(model, Integer.MAX_VALUE);
        XsdTreeNode root = builder.processElement(rootElement, 0, Set.of());
        builder.expandAll();
        return root;
    }

    /**
     * Tree for a DTD: several roots are grouped under a node named {@code label}, and the size is capped because
     * mixed content (every inline element containing every other one) grows factorially with depth.
     */
    public static XsdTreeNode createTree(XsdModel model, List<String> rootElementNames, String label) {
        Builder builder = new Builder(model, MAX_DTD_NODES);
        XsdTreeNode root;
        if (rootElementNames.isEmpty()) {
            return createTree(model, "");
        } else if (rootElementNames.size() == 1) {
            root = builder.processElement(model.getGlobalElements().get(rootElementNames.get(0)), 0, Set.of());
        } else {
            root = new XsdTreeNode(label, label, null, null, false, null, null, false, null);
            for (String name : rootElementNames) {
                root.add(builder.processElement(model.getGlobalElements().get(name), 0, Set.of()));
            }
        }
        builder.expandAll();
        return root;
    }

    // ponytail: hard node cap, lazy expansion on tree-expand if users need the full depth of huge DTDs
    private static final int MAX_DTD_NODES = 50_000;

    private record Pending(XsdTreeNode node, XsdComplexType type, int depth, Set<String> visitedTypes) {}

    /** Builds breadth-first so a node budget truncates evenly instead of starving later branches. */
    private static class Builder {
        private final XsdModel model;
        private final int maxNodes;
        private final ArrayDeque<Pending> queue = new ArrayDeque<>();
        private int nodeCount;

        Builder(XsdModel model, int maxNodes) {
            this.model = model;
            this.maxNodes = maxNodes;
        }

        void expandAll() {
            while (!queue.isEmpty()) {
                Pending p = queue.poll();
                if (nodeCount >= maxNodes) {
                    p.node().add(new XsdTreeNode(MyMessageBundle.message("editor.truncated"), MyMessageBundle.message("editor.truncated"), null, null, false, null, null, false, null));
                } else {
                    addComplexTypeChildren(p.node(), p.type(), p.depth(), p.visitedTypes());
                }
            }
        }

        XsdTreeNode processElement(XsdElement element, int depth, Set<String> visitedTypes) {
            nodeCount++;
            if (depth > 15) {
                return new XsdTreeNode(MyMessageBundle.message("editor.depth",depth), MyMessageBundle.message("editor.depth",depth), null, null, false, null, null, false, null);
            }

            String name = element.getName();
            String fullName = element.getFullName();
            String ref = element.getRef();
            String fullRef = element.getFullRef();
            String type = element.getType();
            String fullType = element.getFullType();
            String minOccurs = element.getMinOccurs();
            String maxOccurs = element.getMaxOccurs();
            XsdComplexType anonymousType = element.getAnonymousType();

            String displayName = (name != null && !name.isEmpty()) ? name : (ref != null && !ref.isEmpty() ? ref : "unknown");
            String fullDisplayName = (fullName != null && !fullName.isEmpty()) ? fullName : (fullRef != null && !fullRef.isEmpty() ? fullRef : "unknown");
            String documentation = element.getDocumentation();

            if (fullRef != null && !fullRef.isEmpty()) {
                XsdElement globalEl = model.getGlobalElements().get(ref);
                if (globalEl != null) {
                    if (globalEl.getFullType() != null && !globalEl.getFullType().isEmpty()) {
                        type = globalEl.getType();
                        fullType = globalEl.getFullType();
                    } else if (globalEl.getAnonymousType() != null) {
                        anonymousType = globalEl.getAnonymousType();
                    }
                    if (documentation == null || documentation.isEmpty()) {
                        documentation = globalEl.getDocumentation();
                    }
                }
            }

            if (documentation == null || documentation.isEmpty()) {
                if (fullType != null && model.getComplexTypes().containsKey(type)) {
                    documentation = model.getComplexTypes().get(type).getDocumentation();
                } else if (fullType != null && model.getSimpleTypes().containsKey(type)) {
                    documentation = model.getSimpleTypes().get(type).getDocumentation();
                }
            }

            XsdTreeNode node = new XsdTreeNode(displayName, fullDisplayName, type, fullType, false, minOccurs, maxOccurs, false, documentation);

            if (fullType != null && !fullType.isEmpty()) {
                if (model.getComplexTypes().containsKey(type)) {
                    if (!visitedTypes.contains(type)) {
                        queue.add(new Pending(node, model.getComplexTypes().get(type), depth + 1, with(visitedTypes, type)));
                    } else {
                        node.add(new XsdTreeNode(MyMessageBundle.message("editor.cyclic",type), MyMessageBundle.message("editor.cyclic",type), null, null, false, null, null, false, null));
                    }
                }
            } else if (anonymousType != null) {
                queue.add(new Pending(node, anonymousType, depth + 1, visitedTypes));
            }

            return node;
        }

        private void addComplexTypeChildren(XsdTreeNode node, XsdComplexType complexType, int depth, Set<String> visitedTypes) {
            String baseType = complexType.getBaseType();
            if (baseType != null && !baseType.isEmpty() && model.getComplexTypes().containsKey(baseType)) {
                if (!visitedTypes.contains(baseType)) {
                    addComplexTypeChildren(node, model.getComplexTypes().get(baseType), depth, with(visitedTypes, baseType));
                }
            }

            for (XsdAttribute attr : complexType.getAttributes()) {
                node.add(new XsdTreeNode("@" + attr.getName(), "@" + attr.getFullName(), attr.getType(), attr.getFullType(), true, null, null, attr.isRequired(), attr.getDocumentation()));
            }

            for (XsdElement nested : complexType.getElements()) {
                node.add(processElement(nested, depth + 1, visitedTypes));
            }
        }

        private static Set<String> with(Set<String> set, String value) {
            Set<String> copy = new HashSet<>(set);
            copy.add(value);
            return copy;
        }
    }

    private static String stripNamespace(String value) {
        if (value == null) return "";
        return value.contains(":") ? value.substring(value.indexOf(':') + 1) : value;
    }

    public String getDisplayName() {
        return XsdViewerSettings.getInstance().isShowNamespaces() ? fullDisplayName : displayName;
    }

    public String getType() {
        return XsdViewerSettings.getInstance().isShowNamespaces() ? fullType : type;
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
