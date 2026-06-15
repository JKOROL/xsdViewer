package fr.korol;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import fr.korol.XsdTreeNode;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.util.List;

public class XsdSearchToolWindowFactory implements ToolWindowFactory {
    public static final String TOOL_WINDOW_ID = "XSD Search Results";

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        toolWindow.setIcon(AllIcons.Actions.Search);
    }

    public static void showResults(@NotNull Project project, @NotNull ToolWindow toolWindow, @NotNull List<XsdTreeNode> results, @NotNull String query, @NotNull XsdFileEditor editor) {
        JPanel panel = new JPanel(new BorderLayout());
        
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
                    editor.focusOnNode(selected);
                }
            }
        });

        panel.add(new JLabel("Results for: " + query), BorderLayout.NORTH);
        panel.add(ScrollPaneFactory.createScrollPane(list), BorderLayout.CENTER);

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "Search: " + query, false);
        
        toolWindow.getContentManager().removeAllContents(true);
        toolWindow.getContentManager().addContent(content);
        toolWindow.show();
    }
}
