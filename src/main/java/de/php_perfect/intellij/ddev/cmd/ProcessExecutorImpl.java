package de.php_perfect.intellij.ddev.cmd;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import de.php_perfect.intellij.ddev.cmd.wsl.WslAware;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

public class ProcessExecutorImpl implements ProcessExecutor {
    public static final Logger LOG = Logger.getInstance(ProcessExecutorImpl.class);

    public @NotNull ProcessOutput executeCommandLine(GeneralCommandLine commandLine, int timeout, boolean loginShell) throws ExecutionException {
        final AtomicReference<ProcessOutput> outputReference = new AtomicReference<>();
        final AtomicReference<ExecutionException> exceptionReference = new AtomicReference<>();

        ProgressManager.getInstance().runProcess(() -> {
            try {
                final GeneralCommandLine patchedCommandLine = WslAware.patchCommandLine(commandLine, loginShell);
                CapturingProcessHandler processHandler = new CapturingProcessHandler(patchedCommandLine);
                ProcessOutput output = processHandler.runProcess(timeout);
                outputReference.set(output);

                LOG.debug("command: " + processHandler.getCommandLine() + " returned: " + output);
            } catch (ExecutionException e) {
                exceptionReference.set(e);
            }
        }, new EmptyProgressIndicator());

        if (exceptionReference.get() != null) {
            throw exceptionReference.get();
        }

        return outputReference.get();
    }
}
