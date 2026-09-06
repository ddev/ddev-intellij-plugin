package de.php_perfect.intellij.ddev.terminal;

import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.expose.ExposedLaunchers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.startup.MutableShellExecOptions;
import org.jetbrains.plugins.terminal.startup.ShellExecOptionsCustomizer;

import java.nio.file.Path;

/**
 * Puts the launchers of the exposed container commands first on the PATH of terminals opened in a
 * DDEV project. The terminal translates the local launcher directory for WSL shells.
 *
 * <p>{@link ShellExecOptionsCustomizer} is experimental, but it is the documented successor of the
 * deprecated {@code LocalTerminalCustomizer}; Plugin Verifier runs against the newest EAP to catch changes.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class DdevTerminalCustomizer implements ShellExecOptionsCustomizer {
    @Override
    public void customizeExecOptions(@NotNull Project project, @NotNull MutableShellExecOptions shellExecOptions) {
        final String launchers = ExposedLaunchers.prepare(project);

        if (launchers != null) {
            shellExecOptions.prependEntryToPATH(Path.of(launchers));
        }
    }
}
