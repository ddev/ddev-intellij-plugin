package de.php_perfect.intellij.ddev.cmd;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class MockProcessExecutor implements ProcessExecutor {
    private final Map<String, ProcessOutput> processList = new HashMap<>();
    private final Map<String, ExecutionException> failureList = new HashMap<>();

    public MockProcessExecutor() {
        this.addProcessOutput("docker info", new ProcessOutput(0));

        if (SystemInfo.isWindows) {
            this.addProcessOutput("where ddev", new ProcessOutput(1));
        } else {
            this.addProcessOutput("which ddev", new ProcessOutput(1));
        }
    }

    public void addProcessOutput(@NotNull String command, @NotNull ProcessOutput processOutput) {
        this.processList.put(command, processOutput);
    }

    public void addProcessFailure(@NotNull String command, @NotNull ExecutionException exception) {
        this.failureList.put(command, exception);
    }

    @Override
    public @NotNull ProcessOutput executeCommandLine(GeneralCommandLine commandLine, int timeout, boolean loginShell) throws ExecutionException {
        String commandLineString = commandLine.getCommandLineString();

        final ExecutionException failure = this.failureList.remove(commandLineString);
        if (failure != null) {
            throw failure;
        }

        if (!this.processList.containsKey(commandLineString)) {
            throw new ExecutionException(String.format("[TEST] Command '%s' was not expected", commandLineString));
        }

        ProcessOutput processOutput = this.processList.get(commandLineString);
        this.processList.remove(commandLineString);
        return processOutput;
    }
}
