package de.php_perfect.intellij.ddev.cmd;

import com.google.common.util.concurrent.UncheckedExecutionException;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import org.jetbrains.annotations.NotNull;

public final class DockerImpl implements Docker {
    @Override
    public boolean isRunning(String workDirectory) {
        try {
            return ProcessExecutor.getInstance().executeCommandLine(
                    new GeneralCommandLine("docker", "info")
                            .withWorkDirectory(workDirectory),
                    5_000,
                    false
            ).getExitCode() == 0;
        } catch (ExecutionException | UncheckedExecutionException e) {
            return false;
        }
    }

    @Override
    public @NotNull String getContext(String workDirectory) {
        try {
            return ProcessExecutor.getInstance().executeCommandLine(
                    new GeneralCommandLine("docker", "context", "show")
                            .withWorkDirectory(workDirectory),
                    5_000,
                    false
            ).getStdout();
        } catch (ExecutionException | UncheckedExecutionException e) {
            return "";
        }
    }
}
