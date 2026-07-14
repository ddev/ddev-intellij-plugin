package de.php_perfect.intellij.ddev.cmd;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface Runner {
    void run(@NotNull GeneralCommandLine commandLine, @NotNull String title);

    void run(@NotNull GeneralCommandLine commandLine, @NotNull String title, @Nullable Runnable afterCompletion);

    void runOnSuccess(@NotNull GeneralCommandLine commandLine, @NotNull String title,
                      @Nullable Runnable afterSuccessfulCompletion);

    void runWithOutcome(@NotNull GeneralCommandLine commandLine, @NotNull String title,
                        @Nullable Runnable afterSuccessfulCompletion,
                        @Nullable Runnable afterFailedCompletion);

    void runWithOutcome(@NotNull GeneralCommandLine commandLine, @NotNull String title,
                        byte @Nullable [] standardInput,
                        @Nullable Runnable afterSuccessfulCompletion,
                        @Nullable Runnable afterFailedCompletion);

    void run(@NotNull GeneralCommandLine commandLine, @NotNull String title, @Nullable Runnable afterCompletion, @Nullable Consumer<ProcessHandler> processHandlerConsumer);

    static Runner getInstance(@NotNull Project project) {
        return project.getService(Runner.class);
    }
}
