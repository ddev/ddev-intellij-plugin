package de.php_perfect.intellij.ddev.notification;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public interface DdevNotifier {
    void notifyInstallDdev();

    void notifyNewVersionAvailable(@NotNull String currentVersion, @NotNull String newVersion);

    void notifyAlreadyLatestVersion();

    void notifyMissingPlugin(@NotNull String pluginName, @NotNull String featureName);

    void notifyMissingPlugins(@NotNull String pluginNames, @NotNull String featureName);

    void notifyPhpInterpreterUpdated(@NotNull String phpVersion);

    void notifyUnknownStateEntered();

    void notifyErrorReportSent(@NotNull String id);

    void notifyDockerNotAvailable(final @NotNull String context);

    void notifyAddOnListFailed();

    void notifySnapshotListFailed();

    void notifySnapshotDeleteFailed(@NotNull String name, @NotNull String detail);

    void notifyShareUrl(@NotNull String url);

    void notifyShareFailed(@NotNull String errorCode);

    void notifyShareToolMissing(@NotNull String tool);

    void notifyDockerProviderStartFailed(@NotNull String detail);

    void notifyDdevUpgraded(@NotNull String previousVersion, @NotNull String currentVersion);

    void notifyWordPressShareSetupFailed(@NotNull String detail);

    static DdevNotifier getInstance(@NotNull Project project) {
        return project.getService(DdevNotifier.class);
    }
}
