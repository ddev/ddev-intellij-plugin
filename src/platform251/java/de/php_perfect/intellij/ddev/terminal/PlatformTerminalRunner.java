package de.php_perfect.intellij.ddev.terminal;

import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.jediterm.core.util.TermSize;
import com.jediterm.terminal.ProcessTtyConnector;
import com.jediterm.terminal.TtyConnector;
import com.pty4j.PtyProcess;
import com.pty4j.WinSize;
import com.pty4j.unix.UnixPtyProcess;
import de.php_perfect.intellij.ddev.cmd.wsl.WslAware;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.AbstractTerminalRunner;
import org.jetbrains.plugins.terminal.ShellStartupOptions;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 2025.1 and 2025.2 lack createTtyConnector(options).
 */
@SuppressWarnings("deprecation")
abstract class PlatformTerminalRunner extends AbstractTerminalRunner<Process> {
    private static final Logger LOG = Logger.getInstance(PlatformTerminalRunner.class);

    PlatformTerminalRunner(@NotNull Project project) {
        super(project);
    }

    protected abstract @NotNull List<String> createSessionCommand() throws ExecutionException;

    @Override
    public @NotNull Process createProcess(@NotNull ShellStartupOptions startupOptions) throws ExecutionException {
        return this.createSessionProcess();
    }

    @Override
    public @NotNull TtyConnector createTtyConnector(@NotNull Process process) {
        return this.createSessionConnector(process);
    }

    protected @NotNull PtyProcess createSessionProcess() throws ExecutionException {
        final PtyCommandLine commandLine = new PtyCommandLine(this.createSessionCommand())
                .withConsoleMode(false);

        commandLine.setWorkDirectory(this.myProject.getBasePath());

        // Wrap WSL patching in progress indicator context to avoid "no ProgressIndicator" errors
        final AtomicReference<PtyCommandLine> patchedCommandLineRef = new AtomicReference<>();
        ProgressManager.getInstance().runProcess(
                () -> patchedCommandLineRef.set(WslAware.patchCommandLine(commandLine)),
                new EmptyProgressIndicator()
        );

        try {
            return (PtyProcess) patchedCommandLineRef.get().createProcess();
        } catch (com.intellij.execution.ExecutionException e) {
            throw new ExecutionException("Opening DDEV Terminal failed", e);
        }
    }

    protected @NotNull TtyConnector createSessionConnector(@NotNull Process process) {
        return new DdevTtyConnector((PtyProcess) process);
    }

    /**
     * Extends jediterm's {@link ProcessTtyConnector} rather than the platform's PtyProcessTtyConnector,
     * whose constructor signature differs between IDE versions within the supported build range.
     */
    private static final class DdevTtyConnector extends ProcessTtyConnector {
        private final PtyProcess process;

        private DdevTtyConnector(@NotNull PtyProcess process) {
            super(process, StandardCharsets.UTF_8);
            this.process = process;
        }

        @Override
        public String getName() {
            return "Local";
        }

        @Override
        public void resize(@NotNull TermSize termSize) {
            if (isConnected()) {
                process.setWinSize(new WinSize(termSize.getColumns(), termSize.getRows()));
            }
        }

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
    }
}
