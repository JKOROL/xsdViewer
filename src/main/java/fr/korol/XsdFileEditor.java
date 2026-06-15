package fr.korol;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.treeStructure.Tree;
import fr.korol.model.XsdModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.beans.PropertyChangeListener;

public class XsdFileEditor extends UserDataHolderBase implements FileEditor {
    private final Project project;
    private final VirtualFile file;
    private final JComponent component;

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
            Tree tree = new Tree(new DefaultTreeModel(rootNode));
            tree.setRootVisible(true);
            tree.setCellRenderer(new XsdTreeNode.XsdTreeCellRenderer());

            return ScrollPaneFactory.createScrollPane(tree);
        } catch (Exception e) {
            return new JLabel("Error reading file: " + e.getMessage());
        }
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
        return "XSD Tree";
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
