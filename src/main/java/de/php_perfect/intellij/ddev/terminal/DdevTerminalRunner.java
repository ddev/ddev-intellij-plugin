package de.php_perfect.intellij.ddev.terminal;

import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.terminal.pty.PtyProcessTtyConnector;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.jediterm.terminal.TtyConnector;
import com.pty4j.PtyProcess;
import com.pty4j.unix.UnixPtyProcess;
import de.php_perfect.intellij.ddev.cmd.wsl.WslAware;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.AbstractTerminalRunner;
import org.jetbrains.plugins.terminal.ShellStartupOptions;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public final class DdevTerminalRunner extends AbstractTerminalRunner<PtyProcess> {
    private static final Logger LOG = Logger.getInstance(DdevTerminalRunner.class);

    public DdevTerminalRunner(@NotNull Project project) {
        super(project);
    }

    @Override
    public @NotNull PtyProcess createProcess(@NotNull ShellStartupOptions startupOptions) throws ExecutionException {
        State ddevState = DdevStateManager.getInstance(this.myProject).getState();

        if (!ddevState.isAvailable()) {
            throw new ExecutionException("DDEV not installed", null);
        }

        final PtyCommandLine commandLine = new PtyCommandLine(List.of(Objects.requireNonNull(ddevState.getDdevBinary()), "ssh"))
                .withConsoleMode(false);

        commandLine.setWorkDirectory(getProject().getBasePath());

        final PtyCommandLine patchedCommandLine = WslAware.patchCommandLine(commandLine);

        try {
            return (PtyProcess) patchedCommandLine.createProcess();
        } catch (com.intellij.execution.ExecutionException e) {
            throw new ExecutionException("Opening DDEV Terminal failed", e);
        }
    }

    @Override
    public @NotNull TtyConnector createTtyConnector(@NotNull PtyProcess process) {
        return new PtyProcessTtyConnector(process, StandardCharsets.UTF_8) {
            @Override
            public void close() {
                if (process instanceof UnixPtyProcess unixPtyProcess) {
                    unixPtyProcess.hangup();
                    AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> {
                        if (process.isAlive()) {
                            LOG.info("Terminal hasn't been terminated by SIGHUP, performing default termination");
                            process.destroy();
                        }
                    }, 1000, TimeUnit.MILLISECONDS);
                } else {
                    process.destroy();
                }
            }
        };
    }

    @Override
    public @NlsContexts.TabTitle String getDefaultTabTitle() {
        return "DDEV Web Container";
    }

    @Override
    public boolean isTerminalSessionPersistent() {
        return false;
    }
}
