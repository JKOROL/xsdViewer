package fr.korol;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import fr.korol.model.XsdModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class XsdFileEditor extends UserDataHolderBase implements FileEditor {
    private final Project project;
    private final VirtualFile file;
    private final JComponent component;
    private Tree tree;

    public XsdFileEditor(@NotNull Project project, @NotNull VirtualFile file) {
        this.project = project;
        this.file = file;
        this.component = createComponent();
    }

    private JComponent createComponent() {
        try {
            XsdParser parser = new XsdParser();
            XsdModel model = parser.parse(file);
            String rootName = file.getNameWithoutExtension();
            // Handle UBL naming convention like UBL-ApplicationResponse-2.1 -> ApplicationResponse
            if (rootName.startsWith("UBL-")) {
                rootName = rootName.substring(4);
                if (rootName.contains("-")) {
                    rootName = rootName.substring(0, rootName.indexOf("-"));
                }
            }

            XsdTreeNode rootNode = XsdTreeNode.createTree(model, rootName);
            tree = new Tree(new DefaultTreeModel(rootNode)) {
                @Override
                public String getToolTipText(java.awt.event.MouseEvent event) {
                    if (getRowForLocation(event.getX(), event.getY()) == -1) return null;
                    TreePath path = getPathForLocation(event.getX(), event.getY());
                    if (path != null && path.getLastPathComponent() instanceof XsdTreeNode node) {
                        return node.getDocumentation();
                    }
                    return super.getToolTipText(event);
                }
            };
            ToolTipManager.sharedInstance().registerComponent(tree);
            tree.setRootVisible(true);
            tree.setCellRenderer(new XsdTreeNode.XsdTreeCellRenderer());

            JPanel panel = new JPanel(new BorderLayout());
            SearchTextField searchTextField = new SearchTextField();
            searchTextField.addKeyboardListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                        performSearch(searchTextField.getText());
                    }
                }
            });

            panel.add(searchTextField, BorderLayout.NORTH);
            panel.add(ScrollPaneFactory.createScrollPane(tree), BorderLayout.CENTER);

            searchTextField.setToolTipText(MyMessageBundle.message("search.placeholder"));

            return panel;
        } catch (Exception e) {
            return new JLabel(MyMessageBundle.message("error.reading.file", e.getMessage()));
        }
    }

    private void performSearch(String text) {
        if (text == null || text.isEmpty() || tree == null) return;

        List<XsdTreeNode> results = new ArrayList<>();
        XsdTreeNode root = (XsdTreeNode) tree.getModel().getRoot();
        searchNodes(root, text.toLowerCase(), results);

        if (results.isEmpty()) {
            JOptionPane.showMessageDialog(component, MyMessageBundle.message("search.no.results", text));
            return;
        }

        showSearchResults(results, text);
    }

    private void searchNodes(XsdTreeNode node, String text, List<XsdTreeNode> results) {
        if (node.getDisplayName().toLowerCase().contains(text)) {
            results.add(node);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            searchNodes((XsdTreeNode) node.getChildAt(i), text, results);
        }
    }

    private void showSearchResults(List<XsdTreeNode> results, String query) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(XsdSearchToolWindowFactory.TOOL_WINDOW_ID);
        if (toolWindow != null) {
            XsdSearchToolWindowFactory.showResults(project, toolWindow, results, query, this);
        } else {
            // Fallback to JDialog if tool window is not found
            JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(component), MyMessageBundle.message("search.results.title", query));
            dialog.setLayout(new BorderLayout());

            DefaultListModel<XsdTreeNode> listModel = new DefaultListModel<>();
            for (XsdTreeNode node : results) {
                listModel.addElement(node);
            }

            JList<XsdTreeNode> list = new JList<>(listModel);
            list.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof XsdTreeNode node) {
                        StringBuilder pathBuilder = new StringBuilder();
                        TreeNode[] path = node.getPath();
                        for (int i = 0; i < path.length; i++) {
                            if (path[i] instanceof XsdTreeNode pNode) {
                                pathBuilder.append(pNode.getDisplayName());
                                if (i < path.length - 1) {
                                    pathBuilder.append("/");
                                }
                            }
                        }
                        setText(pathBuilder.toString() + (node.getType() != null ? " : " + node.getType() : ""));
                    }
                    return this;
                }
            });

            list.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    XsdTreeNode selected = list.getSelectedValue();
                    if (selected != null) {
                        focusOnNode(selected);
                    }
                }
            });

            dialog.add(ScrollPaneFactory.createScrollPane(list), BorderLayout.CENTER);
            dialog.setSize(400, 300);
            dialog.setLocationRelativeTo(component);
            dialog.setVisible(true);
        }
    }

    public void focusOnNode(XsdTreeNode node) {
        TreeNode[] nodes = node.getPath();
        TreePath path = new TreePath(nodes);
        
        // Ensure all parent nodes are expanded
        for (int i = 0; i < nodes.length - 1; i++) {
            TreeNode[] parentNodes = new TreeNode[i + 1];
            System.arraycopy(nodes, 0, parentNodes, 0, i + 1);
            tree.expandPath(new TreePath(parentNodes));
        }

        tree.setSelectionPath(path);
        tree.scrollPathToVisible(path);
    }

    @Override
    public @NotNull JComponent getComponent() {
        return component;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return component;
    }

    @Override
    public @NotNull String getName() {
        return MyMessageBundle.message("editor.name");
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
    }

    @Override
    public @NotNull VirtualFile getFile() {
        return file;
    }
}
