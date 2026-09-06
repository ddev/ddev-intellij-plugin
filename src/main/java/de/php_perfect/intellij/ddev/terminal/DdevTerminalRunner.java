package de.php_perfect.intellij.ddev.terminal;

import de.php_perfect.intellij.ddev.util.DdevProjectRoot;
import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.terminal.pty.PtyProcessTtyConnector;
import com.jediterm.terminal.TtyConnector;
import de.php_perfect.intellij.ddev.cmd.wsl.WslAware;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.terminal.AbstractTerminalRunner;
import org.jetbrains.plugins.terminal.ShellStartupOptions;

import java.lang.reflect.Constructor;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

public final class DdevTerminalRunner extends AbstractTerminalRunner<Process> {
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
    public @NotNull TtyConnector createTtyConnector(@NotNull ShellStartupOptions startupOptions) throws ExecutionException {
        final Process process = this.createDdevProcess();

        return new HangupOnCloseTtyConnector(createPtyConnector(process), process);
    }

    /**
     * IntelliJ 2026.2 declares {@code PtyProcessTtyConnector(PtyProcess, Charset)} and later builds
     * {@code PtyProcessTtyConnector(Process, Charset)}, so the constructor of the running IDE is looked up.
     */
    static @NotNull TtyConnector createPtyConnector(@NotNull Process process) throws ExecutionException {
        for (Constructor<?> constructor : PtyProcessTtyConnector.class.getConstructors()) {
            final Class<?>[] parameters = constructor.getParameterTypes();
            if (parameters.length == 2 && parameters[0].isInstance(process) && parameters[1] == Charset.class) {
                try {
                    return (TtyConnector) constructor.newInstance(process, StandardCharsets.UTF_8);
                } catch (ReflectiveOperationException exception) {
                    throw new ExecutionException("Opening DDEV Terminal failed", exception);
                }
            }
        }
        throw new ExecutionException("No terminal connector accepts " + process.getClass().getName(), null);
    }

    private @NotNull Process createDdevProcess() throws ExecutionException {
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
            return patchedCommandLineRef.get().createProcess();
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
}
