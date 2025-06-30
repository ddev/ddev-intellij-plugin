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

    void notifyDdevDetected(@NotNull String binary);

    void notifyDockerNotAvailable(final @NotNull String context);

    static DdevNotifier getInstance(@NotNull Project project) {
        return project.getService(DdevNotifier.class);
    }
}
