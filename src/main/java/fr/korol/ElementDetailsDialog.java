package fr.korol;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.Enumeration;
import javax.swing.tree.TreeNode;
import fr.korol.model.XsdModel;
import fr.korol.model.XsdSimpleType;

public class ElementDetailsDialog extends DialogWrapper {
    private final XsdTreeNode node;
    private final XsdModel model;

    public ElementDetailsDialog(XsdTreeNode node, XsdModel model) {
        super(true);
        this.node = node;
        this.model = model;
        setTitle(node.getDisplayName());
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Parent Node
        if (node.getParent() instanceof XsdTreeNode parentNode) {
            JPanel parentPanel = new JPanel(new BorderLayout());
            parentPanel.setBorder(BorderFactory.createTitledBorder("Parent Node"));
            parentPanel.add(new JLabel(parentNode.getDisplayName()));
            parentPanel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        close(CANCEL_EXIT_CODE);
                        new ElementDetailsDialog(parentNode, model).show();
                    }
                }
            });
            mainPanel.add(parentPanel);
            mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        // Description
        if (node.getDocumentation() != null && !node.getDocumentation().isEmpty()) {
            JPanel descPanel = new JPanel(new BorderLayout());
            descPanel.setBorder(BorderFactory.createTitledBorder("Description"));
            JTextArea textArea = new JTextArea(node.getDocumentation());
            textArea.setEditable(false);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            descPanel.add(new JScrollPane(textArea));
            mainPanel.add(descPanel);
            mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        // Children
        DefaultListModel<XsdTreeNode> listModel = new DefaultListModel<>();
        Enumeration<?> children = node.children();
        while (children.hasMoreElements()) {
            Object child = children.nextElement();
            if (child instanceof XsdTreeNode childNode) {
                listModel.addElement(childNode);
            }
        }
        if (!listModel.isEmpty()) {
            JPanel childrenPanel = new JPanel(new BorderLayout());
            childrenPanel.setBorder(BorderFactory.createTitledBorder("Child Nodes"));
            JList<XsdTreeNode> childList = new JList<>(listModel);
            childList.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof XsdTreeNode node) {
                        setText(node.getDisplayName());
                    }
                    return this;
                }
            });
            childList.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        XsdTreeNode selectedNode = childList.getSelectedValue();
                        if (selectedNode != null) {
                            close(CANCEL_EXIT_CODE);
                            new ElementDetailsDialog(selectedNode, model).show();
                        }
                    }
                }
            });
            childrenPanel.add(new JScrollPane(childList), BorderLayout.CENTER);
            mainPanel.add(childrenPanel);
            mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        }
        // Restrictions
        if (node.getType() != null && !node.getType().isEmpty()) {
            XsdSimpleType simpleType = model.getSimpleTypes().get(node.getType());
            if (simpleType != null) {
                JPanel detailsPanel = new JPanel();
                detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
                detailsPanel.setBorder(BorderFactory.createTitledBorder("Restrictions/Enumerations"));
                
                if (!simpleType.getRestrictions().isEmpty()) {
                    JPanel restrictionsPanel = new JPanel(new BorderLayout());
                    restrictionsPanel.add(new JLabel("Restrictions:"), BorderLayout.NORTH);
                    StringBuilder sb = new StringBuilder();
                    simpleType.getRestrictions().forEach((k, v) -> sb.append(k).append(": ").append(v).append("\n"));
                    JTextArea textArea = new JTextArea(sb.toString());
                    textArea.setEditable(false);
                    restrictionsPanel.add(new JScrollPane(textArea), BorderLayout.CENTER);
                    detailsPanel.add(restrictionsPanel);
                }
                
                if (!simpleType.getEnumerations().isEmpty()) {
                    JPanel enumerationsPanel = new JPanel(new BorderLayout());
                    enumerationsPanel.add(new JLabel("Enumerations:"), BorderLayout.NORTH);
                    StringBuilder sb = new StringBuilder();
                    simpleType.getEnumerations().forEach((v, d) -> {
                        sb.append(v);
                        if (d != null && !d.isEmpty()) {
                            sb.append(" -> ").append(d.replace("\n", " "));
                        }
                        sb.append("\n");
                    });
                    JTextArea textArea = new JTextArea(sb.toString());
                    textArea.setEditable(false);
                    enumerationsPanel.add(new JScrollPane(textArea), BorderLayout.CENTER);
                    detailsPanel.add(enumerationsPanel);
                }
                mainPanel.add(detailsPanel);
            }
        }
        
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setPreferredSize(new Dimension(450, 500));
        return scrollPane;
    }
}
