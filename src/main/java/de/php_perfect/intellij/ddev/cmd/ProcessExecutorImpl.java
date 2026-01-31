package de.php_perfect.intellij.ddev.cmd;

import com.google.common.util.concurrent.UncheckedExecutionException;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import de.php_perfect.intellij.ddev.cmd.wsl.WslAware;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class ProcessExecutorImpl implements ProcessExecutor {
    public static final Logger LOG = Logger.getInstance(ProcessExecutorImpl.class);
    private static final String FAILED_EXECUTE_COMMAND_MSG = "Failed to execute command: ";

    public @NotNull ProcessOutput executeCommandLine(GeneralCommandLine commandLine, int timeout, boolean loginShell) throws ExecutionException {
        // Check if we're on the EDT
        if (ApplicationManager.getApplication().isDispatchThread()) {
            // If we're on the EDT, run the command in a background thread
            return executeCommandLineInBackground(commandLine, timeout, loginShell);
        } else {
            // If we're already on a background thread, run the command directly
            return executeCommandLineDirectly(commandLine, timeout, loginShell);
        }
    }

    private @NotNull ProcessOutput executeCommandLineDirectly(GeneralCommandLine commandLine, int timeout, boolean loginShell) throws ExecutionException {
        final GeneralCommandLine patchedCommandLine = WslAware.patchCommandLine(commandLine, loginShell);

        try {
            CapturingProcessHandler processHandler = new CapturingProcessHandler(patchedCommandLine);
            ProcessOutput output = processHandler.runProcess(timeout);

            LOG.debug("command: " + processHandler.getCommandLine() + " returned: " + output);
            return output;
        } catch (ExecutionException e) {
            LOG.error(FAILED_EXECUTE_COMMAND_MSG + patchedCommandLine.getCommandLineString(), e);
            throw e;
        }
    }

    private @NotNull ProcessOutput executeCommandLineInBackground(GeneralCommandLine commandLine, int timeout, boolean loginShell) throws ExecutionException {
        final GeneralCommandLine patchedCommandLine = WslAware.patchCommandLine(commandLine, loginShell);
        final AtomicReference<ProcessOutput> outputReference = new AtomicReference<>();
        final AtomicReference<ExecutionException> exceptionReference = new AtomicReference<>();

        // Create a future that will be completed when the task is done
        Future<ProcessOutput> future = ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                CapturingProcessHandler processHandler = new CapturingProcessHandler(patchedCommandLine);
                ProcessOutput output = processHandler.runProcess(timeout);
                outputReference.set(output);

                LOG.debug("command: " + processHandler.getCommandLine() + " returned: " + output);
                return output;
            } catch (ExecutionException e) {
                LOG.error(FAILED_EXECUTE_COMMAND_MSG + patchedCommandLine.getCommandLineString(), e);
                exceptionReference.set(e);
                throw new UncheckedExecutionException(e);
            }
        });

        try {
            // Wait for the future to complete with a timeout
            future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOG.error(FAILED_EXECUTE_COMMAND_MSG + patchedCommandLine.getCommandLineString(), e);
            future.cancel(true);
            // Restore the interrupted status
            Thread.currentThread().interrupt();
            throw new ExecutionException(FAILED_EXECUTE_COMMAND_MSG + e.getMessage(), e);
        } catch (java.util.concurrent.ExecutionException | TimeoutException e) {
            LOG.error(FAILED_EXECUTE_COMMAND_MSG + patchedCommandLine.getCommandLineString(), e);
            future.cancel(true);
            throw new ExecutionException(FAILED_EXECUTE_COMMAND_MSG + e.getMessage(), e);
        }

        // Check if an exception was thrown
        if (exceptionReference.get() != null) {
            throw exceptionReference.get();
        }

        // Return the output
        ProcessOutput output = outputReference.get();
        if (output == null) {
            throw new ExecutionException(FAILED_EXECUTE_COMMAND_MSG + "No output returned");
        }

        return output;
    }
}
