package de.php_perfect.intellij.ddev.php;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import de.php_perfect.intellij.ddev.cmd.Description;
import de.php_perfect.intellij.ddev.dockerCompose.DdevComposeFileLoader;
import de.php_perfect.intellij.ddev.settings.DdevSettingsState;
import de.php_perfect.intellij.ddev.util.FeatureRequiredPlugins;
import de.php_perfect.intellij.ddev.util.PluginChecker;
import org.jetbrains.annotations.NotNull;

public final class ConfigurationProviderImpl implements ConfigurationProvider {

    private final @NotNull Project project;

    public ConfigurationProviderImpl(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public void configure(@NotNull Description description) {
        if (!DdevSettingsState.getInstance(this.project).autoConfigurePhpInterpreter) {
            return;
        }

        if (description.getName() == null || description.getPhpVersion() == null) {
            return;
        }

        final VirtualFile composeFile = DdevComposeFileLoader.getInstance(this.project).load();

        if (composeFile == null || !composeFile.exists()) {
            return;
        }

        if (PluginChecker.isMissingRequiredPlugins(this.project, FeatureRequiredPlugins.PHP_INTERPRETER, "PHP interpreter auto-registration")) {
            return;
        }

        final DdevInterpreterConfig ddevInterpreterConfig = new DdevInterpreterConfig(description.getName(), "php" + description.getPhpVersion(), composeFile.getPath());
        PhpInterpreterProvider.getInstance(this.project).registerInterpreter(ddevInterpreterConfig);
    }
}
