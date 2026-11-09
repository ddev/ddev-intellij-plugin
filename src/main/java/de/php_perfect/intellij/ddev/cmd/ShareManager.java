package de.php_perfect.intellij.ddev.cmd;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Tracks the project's running {@code ddev share} process for stopping.
 */
@Service(Service.Level.PROJECT)
public final class ShareManager {
    private final @NotNull AtomicReference<ProcessHandler> shareProcessHandler = new AtomicReference<>();

    public void setShareProcessHandler(@Nullable ProcessHandler processHandler) {
        this.shareProcessHandler.set(processHandler);
    }

    public boolean isSharing() {
        final ProcessHandler processHandler = this.shareProcessHandler.get();

        return processHandler != null && !processHandler.isProcessTerminated();
    }

    public void stopSharing() {
        final ProcessHandler processHandler = this.shareProcessHandler.getAndSet(null);

        if (processHandler != null && !processHandler.isProcessTerminated()) {
            processHandler.destroyProcess();
        }
    }

    public static @NotNull ShareManager getInstance(@NotNull Project project) {
        return project.getService(ShareManager.class);
    }
}
