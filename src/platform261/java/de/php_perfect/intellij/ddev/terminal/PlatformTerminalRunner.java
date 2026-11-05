package de.php_perfect.intellij.ddev.terminal;

import com.intellij.openapi.project.Project;
import com.jediterm.terminal.TtyConnector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.AbstractTerminalRunner;
import org.jetbrains.plugins.terminal.ShellStartupOptions;

import java.util.concurrent.ExecutionException;

abstract class PlatformTerminalRunner extends AbstractTerminalRunner<Process> {
    PlatformTerminalRunner(@NotNull Project project) {
        super(project);
    }

    protected abstract @NotNull Process createSessionProcess() throws ExecutionException;

    protected abstract @NotNull TtyConnector createSessionConnector(@NotNull Process process) throws ExecutionException;

    @Override
    public @NotNull TtyConnector createTtyConnector(@NotNull ShellStartupOptions startupOptions) throws ExecutionException {
        return this.createSessionConnector(this.createSessionProcess());
    }
}
