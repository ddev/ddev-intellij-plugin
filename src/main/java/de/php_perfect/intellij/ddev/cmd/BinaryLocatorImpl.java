package de.php_perfect.intellij.ddev.cmd;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BinaryLocatorImpl implements BinaryLocator {
    private static final @NotNull Logger LOG = Logger.getInstance(BinaryLocatorImpl.class);

    private static final @NotNull String DDEV_COMMAND = "ddev";

    private static final int BINARY_LOOKUP_TIMEOUT = 15_000;

    @Override
    public @Nullable String findInPath(@NotNull Project project) {
        final String projectDir = project.getBasePath();
        final GeneralCommandLine commandLine = new GeneralCommandLine(WhichProvider.getWhichCommand(projectDir), DDEV_COMMAND).withWorkDirectory(projectDir);

        try {
            final ProcessOutput processOutput = ProcessExecutor.getInstance().executeCommandLine(commandLine, BINARY_LOOKUP_TIMEOUT, true);

            if (processOutput.getExitCode() != 0) {
                return null;
            }

            return processOutput.getStdout().strip();
        } catch (ExecutionException exception) {
            LOG.error(exception);
            return null;
        }
    }
}
