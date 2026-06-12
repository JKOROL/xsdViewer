package fr.korol;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.jcef.JBCefBrowser;
import fr.korol.model.XsdModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.nio.charset.StandardCharsets;

public class XsdFileEditor extends UserDataHolderBase implements FileEditor {
    private final Project project;
    private final VirtualFile file;
    private final JBCefBrowser browser;

    public XsdFileEditor(@NotNull Project project, @NotNull VirtualFile file) {
        this.project = project;
        this.file = file;
        this.browser = new JBCefBrowser();
        loadGraph();
    }

    private void loadGraph() {
        try {
            String content = new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
            XsdParser parser = new XsdParser();
            XsdModel model = parser.parse(content);
            MermaidGenerator generator = new MermaidGenerator();
            String mermaidData = generator.generate(model);

            String html = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "    <script src=\"https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js\"></script>\n" +
                    "    <script>\n" +
                    "        mermaid.initialize({ \n" +
                    "            startOnLoad: true,\n" +
                    "            flowchart: {\n" +
                    "                useMaxWidth: false,\n" +
                    "                htmlLabels: true\n" +
                    "            }\n" +
                    "        });\n" +
                    "    </script>\n" +
                    "    <style>\n" +
                    "        body { background-color: white; margin: 20px; }\n" +
                    "        .mermaid { overflow: auto; }\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <div class=\"mermaid\">\n" +
                    "        " + mermaidData + "\n" +
                    "    </div>\n" +
                    "</body>\n" +
                    "</html>";
            browser.loadHTML(html);
        } catch (Exception e) {
            browser.loadHTML("Error reading file: " + e.getMessage());
        }
    }

    @Override
    public @NotNull JComponent getComponent() {
        return browser.getComponent();
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return browser.getComponent();
    }

    @Override
    public @NotNull String getName() {
        return "XSD Graph";
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
        browser.dispose();
    }

    @Override
    public @NotNull VirtualFile getFile() {
        return file;
    }
}
