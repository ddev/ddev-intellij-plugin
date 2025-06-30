package de.php_perfect.intellij.ddev.version;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.notification.DdevNotifier;
import de.php_perfect.intellij.ddev.settings.DdevSettingsState;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import de.php_perfect.intellij.ddev.version.util.VersionCompare;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class VersionCheckerImpl implements VersionChecker {
    private final @NotNull Project project;

    public VersionCheckerImpl(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public void checkDdevVersion() {
        this.checkDdevVersion(false);
    }

    @Override
    public void checkDdevVersion(boolean confirmNewestVersion) {
        final DdevSettingsState settings = DdevSettingsState.getInstance(this.project);

        if (!settings.checkForUpdates) {
            return;
        }

        final State state = DdevStateManager.getInstance(this.project).getState();
        final Version currentVersion = this.getCurrentVersion(state);

        if (currentVersion == null) {
            if (state.isConfigured()) {
                DdevNotifier.getInstance(project).notifyInstallDdev();
            }
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Checking DDEV version", true) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                final LatestRelease latestRelease = ReleaseClient.getInstance().loadCurrentVersion(progressIndicator);
                progressIndicator.checkCanceled();

                if (latestRelease == null || latestRelease.tagName() == null) {
                    return;
                }

                final Version latestVersion = new Version(latestRelease.tagName());

                if (VersionCompare.needsUpdate(currentVersion, latestVersion)) {
                    DdevNotifier.getInstance(project).notifyNewVersionAvailable(currentVersion.toString(), latestVersion.toString());
                } else if (confirmNewestVersion) {
                    DdevNotifier.getInstance(project).notifyAlreadyLatestVersion();
                }
            }
        });
    }

    private @Nullable Version getCurrentVersion(State state) {
        if (!state.isAvailable()) {
            return null;
        }

        return state.getDdevVersion();
    }
}
