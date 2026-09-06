package de.php_perfect.intellij.ddev.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings that apply to every DDEV project the IDE opens.
 */
@State(name = "de.php_perfect.intellij.ddev.settings.DdevApplicationSettings", storages = @Storage("DdevIntegration.xml"))
@Service(Service.Level.APP)
public final class DdevApplicationSettings implements PersistentStateComponent<DdevApplicationSettings> {
    /**
     * Container commands exposed in the terminal of every DDEV project, in addition to the project's own.
     */
    public @NotNull List<String> exposedCommands = new ArrayList<>();

    public static @NotNull DdevApplicationSettings getInstance() {
        return ApplicationManager.getApplication().getService(DdevApplicationSettings.class);
    }

    @Override
    public @NotNull DdevApplicationSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull DdevApplicationSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
