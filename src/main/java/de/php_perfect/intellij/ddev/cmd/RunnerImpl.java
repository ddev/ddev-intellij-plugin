package de.php_perfect.intellij.ddev.cmd;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunContentExecutor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ColoredProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import de.php_perfect.intellij.ddev.cmd.wsl.WslAware;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RunnerImpl implements Runner, Disposable {
    private static final Logger LOG = Logger.getInstance(RunnerImpl.class);

    private final @NotNull Project project;

    public RunnerImpl(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public void run(@NotNull GeneralCommandLine commandLine, @NotNull String title) {
        this.run(commandLine, title, null);
    }

    @Override
    public void run(@NotNull GeneralCommandLine commandLine, @NotNull String title, @Nullable Runnable afterCompletion) {
        // Create process handler on background thread to avoid EDT violations
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final ProcessHandler processHandler = this.createProcessHandler(commandLine);

                // Switch back to EDT for UI operations
                ApplicationManager.getApplication().invokeLater(() -> {
                    final RunContentExecutor runContentExecutor = new RunContentExecutor(this.project, processHandler)
                            .withTitle(title)
                            .withActivateToolWindow(true)
                            .withAfterCompletion(afterCompletion)
                            .withStop(processHandler::destroyProcess, () -> !processHandler.isProcessTerminated());
                    Disposer.register(this, runContentExecutor);
                    runContentExecutor.run();
                }, ModalityState.nonModal());
            } catch (ExecutionException exception) {
                LOG.warn("An error occurred running " + commandLine.getCommandLineString(), exception);
            }
        });
    }

    private @NotNull ProcessHandler createProcessHandler(GeneralCommandLine commandLine) throws ExecutionException {
        final ProcessHandler handler = new ColoredProcessHandler(WslAware.patchCommandLine(commandLine));
        ProcessTerminatedListener.attach(handler);

        return handler;
    }

    @Override
    public void dispose() {
        // Use service as parent disposable for running processes
    }
}
