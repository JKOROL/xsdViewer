package fr.korol;

import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XsdXPathSearcher {

    public static List<XsdTreeNode> search(XsdTreeNode root, String xpath) {
        List<XsdTreeNode> results = new ArrayList<>();
        if (xpath == null || xpath.isEmpty()) return results;

        if (xpath.startsWith("//")) {
            searchRecursive(root, xpath.substring(2), results, true);
        } else if (xpath.startsWith("./")) {
            searchRecursive(root, xpath.substring(2), results, false);
        } else if (xpath.startsWith("/")) {
            // Check if root itself matches the first part
            String[] parts = xpath.substring(1).split("/", 2);
            String firstPart = parts[0];
            String remaining = parts.length > 1 ? parts[1] : "";
            
            // XPath /Root matches the root node.
            if (root.getDisplayName().equalsIgnoreCase(firstPart) || firstPart.equals("*")) {
                if (remaining.isEmpty()) {
                    results.add(root);
                } else {
                    searchRecursive(root, remaining, results, false);
                }
            }
        } else {
            searchRecursive(root, xpath, results, true);
        }
        return results;
    }

    private static void searchRecursive(XsdTreeNode node, String remainingPath, List<XsdTreeNode> results, boolean allowDeepSearch) {
        if (remainingPath.isEmpty()) return;

        if (remainingPath.startsWith("/")) {
            searchRecursive(node, remainingPath.substring(1), results, true);
            return;
        }

        String[] parts = remainingPath.split("/", 2);
        String currentPart = parts[0];
        String nextRemaining = parts.length > 1 ? parts[1] : "";

        List<XsdTreeNode> matches = findMatches(node, currentPart, allowDeepSearch);
        
        for (XsdTreeNode match : matches) {
            if (nextRemaining.isEmpty()) {
                results.add(match);
            } else {
                searchRecursive(match, nextRemaining, results, false);
            }
        }
    }

    private static List<XsdTreeNode> findMatches(XsdTreeNode node, String part, boolean recursive) {
        List<XsdTreeNode> matches = new ArrayList<>();
        if (part.isEmpty()) return matches;
        
        // Handle predicates like [ @name='foo' ] or just [1]
        String nameFilter = part;
        String predicate = null;
        if (part.contains("[") && part.endsWith("]")) {
            int bracketIndex = part.indexOf("[");
            nameFilter = part.substring(0, bracketIndex);
            predicate = part.substring(bracketIndex + 1, part.length() - 1);
        }

        if (recursive) {
            for (int i = 0; i < node.getChildCount(); i++) {
                XsdTreeNode child = (XsdTreeNode) node.getChildAt(i);
                checkAndAdd(child, nameFilter, predicate, matches);
                matches.addAll(findMatches(child, part, true));
            }
        } else {
            for (int i = 0; i < node.getChildCount(); i++) {
                XsdTreeNode child = (XsdTreeNode) node.getChildAt(i);
                checkAndAdd(child, nameFilter, predicate, matches);
            }
        }
        
        // Also check the node itself if it matches the nameFilter and we're at the root of the search
        // but typically XPath /Root starts from the root.
        
        return matches;
    }

    private static void checkAndAdd(XsdTreeNode child, String nameFilter, String predicate, List<XsdTreeNode> matches) {
        boolean nameMatch = nameFilter.equals("*") || child.getDisplayName().equalsIgnoreCase(nameFilter) || 
                           (child.isAttribute() && nameFilter.startsWith("@") && child.getDisplayName().substring(1).equalsIgnoreCase(nameFilter.substring(1)));
        
        if (nameMatch) {
            if (predicate == null || checkPredicate(child, predicate)) {
                matches.add(child);
            }
        }
    }

    private static boolean checkPredicate(XsdTreeNode node, String predicate) {
        predicate = predicate.trim();
        if (predicate.startsWith("@")) {
            // Attribute predicate like [@name='foo']
            Pattern pattern = Pattern.compile("@(\\w+)\\s*=\\s*['\"]([^'\"]+)['\"]");
            Matcher matcher = pattern.matcher(predicate);
            if (matcher.find()) {
                String attrName = matcher.group(1);
                String attrValue = matcher.group(2);
                
                // In our tree, attributes are children. This is tricky for standard XPath.
                // But in XSD tree, maybe we look for a child attribute?
                for (int i = 0; i < node.getChildCount(); i++) {
                    XsdTreeNode child = (XsdTreeNode) node.getChildAt(i);
                    if (child.isAttribute() && child.getDisplayName().substring(1).equalsIgnoreCase(attrName)) {
                        // For XSD, the "value" of an attribute node might be its type or something?
                        // Usually we just want to match the name.
                        return true; 
                    }
                }
            }
        } else if (predicate.matches("\\d+")) {
            // Index predicate [1]
            int index = Integer.parseInt(predicate);
            TreeNode parent = node.getParent();
            if (parent != null) {
                int count = 0;
                for (int i = 0; i < parent.getChildCount(); i++) {
                    if (parent.getChildAt(i).getClass() == node.getClass()) {
                        XsdTreeNode sibling = (XsdTreeNode) parent.getChildAt(i);
                        if (sibling.getDisplayName().equalsIgnoreCase(node.getDisplayName())) {
                            count++;
                            if (sibling == node) {
                                return count == index;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}
