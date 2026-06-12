package fr.korol;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import java.util.Random;

public class MyToolWindowFactory implements ToolWindowFactory {
    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return true;
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        MyToolWindow myToolWindow = new MyToolWindow();
        Content content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false);
        toolWindow.getContentManager().addContent(content);
    }

    public static class MyToolWindow {
        private final JBPanel<JBPanel<?>> content;

        public MyToolWindow() {
            content = new JBPanel<>();
            JBLabel label = new JBLabel(MyMessageBundle.message("toolwindow.MyToolWindow.number.label", "?"));

            content.add(label);
            JButton shuffleButton = new JButton(MyMessageBundle.message("toolwindow.MyToolWindow.shuffle.button"));
            shuffleButton.addActionListener(e -> {
                label.setText(MyMessageBundle.message(
                        "toolwindow.MyToolWindow.number.label", new Random().nextInt(1000)
                ));
            });
            content.add(shuffleButton);
        }

        public JBPanel<JBPanel<?>> getContent() {
            return content;
        }
    }
}
