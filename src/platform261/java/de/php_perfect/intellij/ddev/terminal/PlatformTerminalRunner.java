package de.php_perfect.intellij.ddev.terminal;

import com.intellij.openapi.project.Project;
import com.jediterm.terminal.TtyConnector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.AbstractTerminalRunner;
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner;
import org.jetbrains.plugins.terminal.ShellStartupOptions;

import java.util.List;
import java.util.concurrent.ExecutionException;

abstract class PlatformTerminalRunner extends AbstractTerminalRunner<Process> {
    PlatformTerminalRunner(@NotNull Project project) {
        super(project);
    }

    protected abstract @NotNull List<String> createSessionCommand() throws ExecutionException;

    // The terminal plugin starts the process, inside WSL for WSL projects.
    @Override
    public @NotNull TtyConnector createTtyConnector(@NotNull ShellStartupOptions startupOptions) throws ExecutionException {
        final ShellStartupOptions sessionOptions = startupOptions.builder()
                .shellCommand(this.createSessionCommand())
                .workingDirectory(this.myProject.getBasePath())
                .build();
        final LocalTerminalDirectRunner localRunner = LocalTerminalDirectRunner.createTerminalRunner(this.myProject);

        return localRunner.createTtyConnector(localRunner.configureStartupOptions(sessionOptions));
    }
}
