package de.php_perfect.intellij.ddev.cmd;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tracks the running {@code ddev share} process of a project so it can be stopped again.
 */
@Service(Service.Level.PROJECT)
public final class ShareManager {
    private volatile @Nullable ProcessHandler shareProcessHandler;

    public void setShareProcessHandler(@Nullable ProcessHandler processHandler) {
        this.shareProcessHandler = processHandler;
    }

    public boolean isSharing() {
        final ProcessHandler processHandler = this.shareProcessHandler;

        return processHandler != null && !processHandler.isProcessTerminated();
    }

    public void stopSharing() {
        final ProcessHandler processHandler = this.shareProcessHandler;

        if (processHandler != null && !processHandler.isProcessTerminated()) {
            processHandler.destroyProcess();
        }

        this.shareProcessHandler = null;
    }

    public static @NotNull ShareManager getInstance(@NotNull Project project) {
        return project.getService(ShareManager.class);
    }
}
