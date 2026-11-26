package fr.korol;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class OpenFromLibraryAction extends AnAction implements DumbAware {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByPath(XsdViewerSettings.getInstance().getLibraryPath());
        if (root == null || !root.isDirectory()) {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, XsdViewerConfigurable.class);
            return;
        }

        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("xsd").withRoots(root);
        VirtualFile file = FileChooser.chooseFile(descriptor, project, root);
        if (file != null) open(project, file);
    }

    static void open(@NotNull Project project, @NotNull VirtualFile file) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        manager.openFile(file, true);
        manager.setSelectedEditor(file, "xsd-tree-editor");
    }
}
