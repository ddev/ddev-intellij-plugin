package de.php_perfect.intellij.ddev.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

@State(name = "de.php_perfect.intellij.ddev.settings.DdevSettingsState", storages = @Storage("DdevIntegration.xml"))
@Service(Service.Level.PROJECT)
public final class DdevSettingsState implements PersistentStateComponent<DdevSettingsState> {
    public @NotNull String ddevBinary;
    public boolean checkForUpdates;
    public boolean watchDdev;
    public boolean autoConfigureDataSource;
    public boolean autoConfigurePhpInterpreter;
    public boolean autoConfigureNodeJsInterpreter;
    public boolean createSnapshotOnStop;
    public boolean omitSnapshotOnDelete;
    public boolean deleteDdevFolderOnDelete;
    public boolean automaticallyInstallCms;
    public @NotNull String wordpressTablePrefixImportPolicy;
    public @NotNull String wordpressUrlImportPolicy;
    public @NotNull String projectNameFormat;
    public boolean expandServicesInProjectsToolWindow;
    public boolean showCurrentProjectOnly;

    public DdevSettingsState() {
        // Set default values for new installations
        this.ddevBinary = "";
        this.checkForUpdates = true;
        this.watchDdev = true;
        this.autoConfigureDataSource = true;
        this.autoConfigurePhpInterpreter = true;
        this.autoConfigureNodeJsInterpreter = true;
        this.createSnapshotOnStop = false;
        this.omitSnapshotOnDelete = false;
        this.deleteDdevFolderOnDelete = false;
        this.automaticallyInstallCms = false;
        this.wordpressTablePrefixImportPolicy = "Ask";
        this.wordpressUrlImportPolicy = "Ask";
        this.projectNameFormat = de.php_perfect.intellij.ddev.toolwindow.DdevProjectNameFormatter.DEFAULT;
        this.expandServicesInProjectsToolWindow = true;
        this.showCurrentProjectOnly = false;
    }

    public static @NotNull DdevSettingsState getInstance(Project project) {
        return project.getService(DdevSettingsState.class);
    }

    @Override
    public @NotNull DdevSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull DdevSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
        this.wordpressTablePrefixImportPolicy =
                de.php_perfect.intellij.ddev.wordpress.WordPressImportPolicy
                        .fromValue(this.wordpressTablePrefixImportPolicy).value();
        this.wordpressUrlImportPolicy = de.php_perfect.intellij.ddev.wordpress.WordPressImportPolicy
                .fromValue(this.wordpressUrlImportPolicy).value();
        if (this.projectNameFormat == null || this.projectNameFormat.isBlank()) {
            this.projectNameFormat = de.php_perfect.intellij.ddev.toolwindow.DdevProjectNameFormatter.DEFAULT;
        }
    }
}
