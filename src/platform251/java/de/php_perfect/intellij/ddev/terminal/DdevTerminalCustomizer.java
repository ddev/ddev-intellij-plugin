package de.php_perfect.intellij.ddev.terminal;

import com.intellij.execution.wsl.WslPath;
import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.expose.ExposedLaunchers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.terminal.LocalTerminalCustomizer;

import java.io.File;
import java.util.Map;

/**
 * Puts the launchers of the exposed container commands first on the PATH of terminals opened in a
 * DDEV project.
 *
 * <p>WSL projects are left alone: this API customizes the Windows-side environment and does not
 * translate the launcher directory for the distribution.</p>
 */
@SuppressWarnings("deprecation")
public final class DdevTerminalCustomizer extends LocalTerminalCustomizer {
    @Override
    public String[] customizeCommandAndEnvironment(@NotNull Project project, @Nullable String workingDirectory,
                                                   String @NotNull [] command, @NotNull Map<String, String> envs) {
        final String basePath = project.getBasePath();
        if (basePath != null && WslPath.parseWindowsUncPath(basePath) != null) {
            return command;
        }

        final String launchers = ExposedLaunchers.prepare(project);
        if (launchers != null) {
            prependToPath(envs, launchers);
        }

        return command;
    }

    static void prependToPath(@NotNull Map<String, String> envs, @NotNull String directory) {
        // Windows spells the variable "Path"; the existing key is kept so no second PATH is introduced
        final String key = envs.keySet().stream()
                .filter(name -> name.equalsIgnoreCase("PATH"))
                .findFirst()
                .orElse("PATH");
        final String current = envs.get(key);

        envs.put(key, current == null || current.isEmpty() ? directory : directory + File.pathSeparator + current);
    }
}
