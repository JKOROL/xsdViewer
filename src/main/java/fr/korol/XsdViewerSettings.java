package fr.korol;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@State(
    name = "fr.korol.XsdViewerSettings",
    storages = @Storage("xsdViewerSettings.xml")
)
public class XsdViewerSettings implements PersistentStateComponent<XsdViewerSettings.State> {

    public interface SettingsListener {
        void settingsChanged();
    }

    private final List<SettingsListener> listeners = new ArrayList<>();

    public static class State {
        public boolean showNamespaces = false;
    }

    private State myState = new State();

    public static XsdViewerSettings getInstance() {
        return ApplicationManager.getApplication().getService(XsdViewerSettings.class);
    }

    @Override
    public @Nullable State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        myState = state;
    }

    public boolean isShowNamespaces() {
        return myState.showNamespaces;
    }

    public void setShowNamespaces(boolean showNamespaces) {
        if (myState.showNamespaces != showNamespaces) {
            myState.showNamespaces = showNamespaces;
            notifyListeners();
        }
    }

    public void addSettingsListener(SettingsListener listener) {
        listeners.add(listener);
    }

    public void removeSettingsListener(SettingsListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (SettingsListener listener : listeners) {
            listener.settingsChanged();
        }
    }
}
