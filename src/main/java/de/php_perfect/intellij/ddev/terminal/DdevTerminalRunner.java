package de.php_perfect.intellij.ddev.terminal;

import de.php_perfect.intellij.ddev.util.DdevProjectRoot;
import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.jediterm.core.util.TermSize;
import com.jediterm.terminal.ProcessTtyConnector;
import com.jediterm.terminal.TtyConnector;
import com.pty4j.PtyProcess;
import com.pty4j.WinSize;
import com.pty4j.unix.UnixPtyProcess;
import de.php_perfect.intellij.ddev.cmd.wsl.WslAware;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class DdevTerminalRunner extends PlatformTerminalRunner {
    private static final Logger LOG = Logger.getInstance(DdevTerminalRunner.class);

    private final @NotNull List<String> ddevArguments;
    private final @NotNull @NlsContexts.TabTitle String tabTitle;
    private final @Nullable String workingDirectory;
    // Set when the tab runs a plain command instead of the DDEV binary.
    private final @Nullable List<String> command;

    public DdevTerminalRunner(@NotNull Project project) {
        this(project, List.of("ssh"), "DDEV Web Container", null);
    }

    public DdevTerminalRunner(@NotNull Project project, @NotNull List<String> ddevArguments, @NotNull @NlsContexts.TabTitle String tabTitle) {
        this(project, ddevArguments, tabTitle, null);
    }

    public DdevTerminalRunner(@NotNull Project project, @NotNull List<String> ddevArguments,
                              @NotNull @NlsContexts.TabTitle String tabTitle, @Nullable String workingDirectory) {
        this(project, ddevArguments, tabTitle, workingDirectory, null);
    }

    private DdevTerminalRunner(@NotNull Project project, @NotNull List<String> ddevArguments,
                               @NotNull @NlsContexts.TabTitle String tabTitle, @Nullable String workingDirectory,
                               @Nullable List<String> command) {
        super(project);
        this.ddevArguments = ddevArguments;
        this.tabTitle = tabTitle;
        this.workingDirectory = workingDirectory;
        this.command = command;
    }

    public static @NotNull DdevTerminalRunner forCommand(@NotNull Project project, @NotNull List<String> command,
                                                         @NotNull @NlsContexts.TabTitle String tabTitle,
                                                         @Nullable String workingDirectory) {
        return new DdevTerminalRunner(project, List.of(), tabTitle, workingDirectory, List.copyOf(command));
    }

    @Override
    protected @NotNull TtyConnector createSessionConnector(@NotNull Process process) {
        return new DdevTtyConnector((PtyProcess) process);
    }

    @Override
    protected @NotNull PtyProcess createSessionProcess() throws ExecutionException {
        final List<String> command = new ArrayList<>();
        if (this.command != null) {
            command.addAll(this.command);
        } else {
            final State ddevState = DdevStateManager.getInstance(this.myProject).getState();

            if (!ddevState.isAvailable()) {
                throw new ExecutionException("DDEV not installed", null);
            }

            command.add(Objects.requireNonNull(ddevState.getDdevBinary()));
            command.addAll(this.ddevArguments);
        }

        final PtyCommandLine commandLine = new PtyCommandLine(command)
                .withConsoleMode(false);

        commandLine.setWorkDirectory(this.workingDirectory != null ? this.workingDirectory : DdevProjectRoot.of(getProject()));

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

    @Override
    public @NlsContexts.TabTitle String getDefaultTabTitle() {
        return this.tabTitle;
    }

    @Override
    public boolean isTerminalSessionPersistent() {
        return false;
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
