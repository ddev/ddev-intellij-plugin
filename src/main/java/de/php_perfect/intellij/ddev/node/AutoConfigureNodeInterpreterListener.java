package de.php_perfect.intellij.ddev.node;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import de.php_perfect.intellij.ddev.DescriptionChangedListener;
import de.php_perfect.intellij.ddev.cmd.Description;
import de.php_perfect.intellij.ddev.dockerCompose.DdevComposeFileLoader;
import de.php_perfect.intellij.ddev.settings.DdevSettingsState;
import de.php_perfect.intellij.ddev.util.FeatureRequiredPlugins;
import de.php_perfect.intellij.ddev.util.PluginChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AutoConfigureNodeInterpreterListener implements DescriptionChangedListener {

    private final @NotNull Project project;

    public AutoConfigureNodeInterpreterListener(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public void onDescriptionChanged(@Nullable Description description) {
        if (description == null || description.getName() == null) {
            return;
        }

        if (!DdevSettingsState.getInstance(this.project).autoConfigureNodeJsInterpreter) {
            return;
        }

        final VirtualFile composeFile = DdevComposeFileLoader.getInstance(this.project).load();

        if (composeFile == null || !composeFile.exists()) {
            return;
        }

        if (PluginChecker.isMissingRequiredPlugins(this.project, FeatureRequiredPlugins.NODE_INTERPRETER, "Node.js interpreter auto-registration")) {
            return;
        }

        final NodeInterpreterConfig nodeInterpreterConfig = new NodeInterpreterConfig(description.getName(), composeFile.getPath(), "node");
        NodeInterpreterProvider.getInstance(this.project).configureNodeInterpreter(nodeInterpreterConfig);
    }
}
