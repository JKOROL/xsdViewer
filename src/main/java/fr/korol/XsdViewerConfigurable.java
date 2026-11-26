package fr.korol;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class XsdViewerConfigurable implements Configurable {

    private JBCheckBox showNamespacesCheckBox;
    private TextFieldWithBrowseButton libraryPathField;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "XSD Viewer";
    }

    @Override
    public @Nullable JComponent createComponent() {
        showNamespacesCheckBox = new JBCheckBox(MyMessageBundle.message("settings.show.namespaces"));
        libraryPathField = new TextFieldWithBrowseButton();
        libraryPathField.addBrowseFolderListener(null, FileChooserDescriptorFactory.createSingleFolderDescriptor());
        return FormBuilder.createFormBuilder()
                .addComponent(showNamespacesCheckBox)
                .addLabeledComponent(MyMessageBundle.message("settings.library.path"), libraryPathField)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    @Override
    public boolean isModified() {
        XsdViewerSettings settings = XsdViewerSettings.getInstance();
        return showNamespacesCheckBox.isSelected() != settings.isShowNamespaces()
                || !libraryPathField.getText().equals(settings.getLibraryPath());
    }

    @Override
    public void apply() {
        XsdViewerSettings settings = XsdViewerSettings.getInstance();
        settings.setShowNamespaces(showNamespacesCheckBox.isSelected());
        settings.setLibraryPath(libraryPathField.getText());
    }

    @Override
    public void reset() {
        XsdViewerSettings settings = XsdViewerSettings.getInstance();
        showNamespacesCheckBox.setSelected(settings.isShowNamespaces());
        libraryPathField.setText(settings.getLibraryPath());
    }
}
