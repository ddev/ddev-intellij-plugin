package de.php_perfect.intellij.ddev.version;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ReleaseClient {
    @Nullable LatestRelease loadCurrentVersion(@NotNull ProgressIndicator indicator);

    static ReleaseClient getInstance() {
        return ApplicationManager.getApplication().getService(ReleaseClient.class);
    }
}
