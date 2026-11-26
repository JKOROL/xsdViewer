package fr.korol;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.JBList;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class XsdLibraryToolWindowFactory implements ToolWindowFactory, DumbAware {

    private record Entry(Path file, String label) {}

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        DefaultListModel<Entry> listModel = new DefaultListModel<>();
        JBList<Entry> list = new JBList<>(listModel);
        list.getEmptyText().setText(MyMessageBundle.message("library.empty"));
        list.setCellRenderer(SimpleListCellRenderer.create("", Entry::label));

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) openSelected(project, list);
            }
        });
        list.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) openSelected(project, list);
            }
        });

        XsdViewerSettings.SettingsListener listener = () -> reload(listModel);
        XsdViewerSettings.getInstance().addSettingsListener(listener);
        Disposer.register(toolWindow.getDisposable(), () -> XsdViewerSettings.getInstance().removeSettingsListener(listener));

        toolWindow.setTitleActions(List.of(new DumbAwareAction(MyMessageBundle.message("library.refresh"), null, AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                reload(listModel);
            }
        }));

        reload(listModel);
        toolWindow.getContentManager().addContent(
                ContentFactory.getInstance().createContent(ScrollPaneFactory.createScrollPane(list), "", false));
    }

    // ponytail: scans on the EDT, move to a background task if libraries grow to thousands of files
    private static void reload(DefaultListModel<Entry> listModel) {
        listModel.clear();
        String libraryPath = XsdViewerSettings.getInstance().getLibraryPath();
        if (libraryPath.isBlank()) return;
        try {
            Path root = Path.of(libraryPath);
            if (!Files.isDirectory(root)) return;
            try (Stream<Path> files = Files.walk(root)) {
                files.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".xsd"))
                        .sorted()
                        .forEach(p -> listModel.addElement(new Entry(p, root.relativize(p).toString())));
            }
        } catch (IOException | UncheckedIOException | IllegalArgumentException ignored) {
            // unreadable or invalid folder: show what was found so far
        }
    }

    private static void openSelected(Project project, JBList<Entry> list) {
        Entry entry = list.getSelectedValue();
        if (entry == null) return;
        VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(entry.file());
        if (file != null) OpenFromLibraryAction.open(project, file);
    }
}
