package de.php_perfect.intellij.ddev.notification;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.actions.*;
import org.jetbrains.annotations.NotNull;

public final class DdevNotifierImpl implements DdevNotifier {
    public static final String STICKY = "DdevIntegration.Sticky";
    public static final String NON_STICKY = "DdevIntegration.NonSticky";
    private final @NotNull Project project;

    public DdevNotifierImpl(final @NotNull Project project) {
        this.project = project;
    }

    @Override
    public void notifyInstallDdev() {
        ApplicationManager.getApplication().invokeLater(() -> NotificationGroupManager.getInstance()
                .getNotificationGroup(STICKY)
                .createNotification(
                        DdevIntegrationBundle.message("notification.InstallDdev.title"),
                        DdevIntegrationBundle.message("notification.InstallDdev.text"),
                        NotificationType.INFORMATION
                )
                .addAction(new InstallationInstructionsAction())
                .notify(this.project), ModalityState.nonModal());
    }

    @Override
    public void notifyNewVersionAvailable(final @NotNull String currentVersion, final @NotNull String latestVersion) {
        ApplicationManager.getApplication().invokeLater(() -> NotificationGroupManager.getInstance()
                .getNotificationGroup(NON_STICKY)
                .createNotification(
                        DdevIntegrationBundle.message("notification.NewVersionAvailable.title"),
                        DdevIntegrationBundle.message("notification.NewVersionAvailable.text", currentVersion, latestVersion),
                        NotificationType.INFORMATION
                )
                .addAction(new InstallationInstructionsAction())
                .addAction(new DisableCheckForUpdatesAction())
                .notify(this.project), ModalityState.nonModal());
    }

    @Override
    public void notifyAlreadyLatestVersion() {
        ApplicationManager.getApplication().invokeLater(() -> NotificationGroupManager.getInstance()
                .getNotificationGroup(NON_STICKY)
                .createNotification(
                        DdevIntegrationBundle.message("notification.AlreadyLatestVersion.text"),
                        NotificationType.INFORMATION
                )
                .notify(this.project), ModalityState.nonModal());
    }

    @Override
    public void notifyMissingPlugin(final @NotNull String pluginName, final @NotNull String featureName) {
        ApplicationManager.getApplication().invokeLater(() -> NotificationGroupManager.getInstance()
                .getNotificationGroup(STICKY)
                .createNotification(
                        DdevIntegrationBundle.message("notification.MissingPlugin.title"),
                        DdevIntegrationBundle.message("notification.MissingPlugin.withFeature.text", pluginName, featureName),
                        NotificationType.WARNING
                )
                .addAction(new ManagePluginsAction())
                .notify(this.project), ModalityState.nonModal());
    }

    @Override
    public void notifyMissingPlugins(final @NotNull String pluginNames, final @NotNull String featureName) {
        ApplicationManager.getApplication().invokeLater(() -> NotificationGroupManager.getInstance()
                .getNotificationGroup(STICKY)
                .createNotification(
                        DdevIntegrationBundle.message("notification.MissingPlugins.title"),
                        DdevIntegrationBundle.message("notification.MissingPlugins.withFeature.text", pluginNames, featureName),
                        NotificationType.WARNING
                )
                .addAction(new ManagePluginsAction())
                .notify(this.project), ModalityState.nonModal());
    }

    @Override
    public void notifyPhpInterpreterUpdated(final @NotNull String phpVersion) {
        ApplicationManager.getApplication().invokeLater(() -> NotificationGroupManager.getInstance()
                .getNotificationGroup(NON_STICKY)
                .createNotification(
                        DdevIntegrationBundle.message("notification.InterpreterUpdated.title"),
                        DdevIntegrationBundle.message("notification.InterpreterUpdated.text", phpVersion),
                        NotificationType.INFORMATION
                )
                .notify(this.project), ModalityState.nonModal());
    }

    @Override
    public void notifyUnknownStateEntered() {
        ApplicationManager.getApplication().invokeLater(() -> NotificationGroupManager.getInstance()
                .getNotificationGroup(STICKY)
                .createNotification(
                        DdevIntegrationBundle.message("notification.UnknownStateEntered.title"),
                        DdevIntegrationBundle.message("notification.UnknownStateEntered.text"),
                        NotificationType.WARNING
                )
                .addAction(ActionManager.getInstance().getAction("DdevIntegration.SyncState"))
                .addAction(new ReportIssueAction())
                .notify(this.project), ModalityState.nonModal());
    }

    @Override
    public void notifyErrorReportSent(final @NotNull String reportId) {
        ApplicationManager.getApplication().invokeLater(() -> NotificationGroupManager.getInstance()
                .getNotificationGroup(NON_STICKY)
                .createNotification(
                        DdevIntegrationBundle.message("errorReporting.success.title"),
                        DdevIntegrationBundle.message("errorReporting.success.text", reportId),
                        NotificationType.INFORMATION
                )
                .addAction(new ReportIssueAction())
                .notify(this.project), ModalityState.nonModal());
    }

    @Override
    public void notifyDdevDetected(final @NotNull String binary) {
        ApplicationManager.getApplication().invokeLater(() -> NotificationGroupManager.getInstance()
                .getNotificationGroup(NON_STICKY)
                .createNotification(
                        DdevIntegrationBundle.message("notification.ddevDetected.title"),
                        DdevIntegrationBundle.message("notification.ddevDetected.text", binary),
                        NotificationType.INFORMATION
                )
                .addAction(new ChangeSettingsAction())
                .notify(this.project), ModalityState.nonModal());
    }

    @Override
    public void notifyDockerNotAvailable(final @NotNull String context) {
        ApplicationManager.getApplication().invokeLater(() -> NotificationGroupManager.getInstance()
                .getNotificationGroup(STICKY)
                .createNotification(
                        DdevIntegrationBundle.message("notification.dockerNotAvailable.title"),
                        DdevIntegrationBundle.message("notification.dockerNotAvailable.text", context),
                        NotificationType.WARNING
                )
                .addAction(new ReloadPluginAction())
                .notify(this.project), ModalityState.nonModal());
    }
}
