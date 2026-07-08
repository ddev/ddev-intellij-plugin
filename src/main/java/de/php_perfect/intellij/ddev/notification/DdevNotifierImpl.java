package de.php_perfect.intellij.ddev.notification;

import com.intellij.ide.BrowserUtil;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.actions.*;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.StringSelection;

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
        // Check outside the EDT whether DDEV is managed by Homebrew and can be updated directly.
        final boolean updatableViaHomebrew = UpdateDdevAction.isAvailable();

        ApplicationManager.getApplication().invokeLater(() -> {
            final var notification = NotificationGroupManager.getInstance()
                    .getNotificationGroup(NON_STICKY)
                    .createNotification(
                            DdevIntegrationBundle.message("notification.NewVersionAvailable.title"),
                            DdevIntegrationBundle.message("notification.NewVersionAvailable.text", currentVersion, latestVersion),
                            NotificationType.INFORMATION
                    );

            if (updatableViaHomebrew) {
                notification.addAction(new UpdateDdevAction());
            }

            notification
                    .addAction(new InstallationInstructionsAction())
                    .addAction(new DisableCheckForUpdatesAction())
                    .notify(this.project);
        }, ModalityState.nonModal());
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

    @Override
    public void notifyAddOnListFailed() {
        ApplicationManager.getApplication().invokeLater(() -> NotificationGroupManager.getInstance()
                .getNotificationGroup(NON_STICKY)
                .createNotification(
                        DdevIntegrationBundle.message("notification.AddOnListFailed.title"),
                        DdevIntegrationBundle.message("notification.AddOnListFailed.text"),
                        NotificationType.WARNING
                )
                .notify(this.project), ModalityState.nonModal());
    }

    @Override
    public void notifyShareUrl(final @NotNull String url) {
        ApplicationManager.getApplication().invokeLater(() -> NotificationGroupManager.getInstance()
                .getNotificationGroup(STICKY)
                .createNotification(
                        DdevIntegrationBundle.message("notification.ShareUrl.title"),
                        DdevIntegrationBundle.message("notification.ShareUrl.text", url),
                        NotificationType.INFORMATION
                )
                .addAction(NotificationAction.createSimple(
                        DdevIntegrationBundle.message("notification.ShareUrl.open"),
                        () -> BrowserUtil.browse(url)))
                .addAction(NotificationAction.createSimple(
                        DdevIntegrationBundle.message("notification.ShareUrl.copy"),
                        () -> CopyPasteManager.getInstance().setContents(new StringSelection(url))))
                .notify(this.project), ModalityState.nonModal());
    }

    @Override
    public void notifySnapshotListFailed() {
        ApplicationManager.getApplication().invokeLater(() -> NotificationGroupManager.getInstance()
                .getNotificationGroup(NON_STICKY)
                .createNotification(
                        DdevIntegrationBundle.message("notification.SnapshotListFailed.title"),
                        DdevIntegrationBundle.message("notification.SnapshotListFailed.text"),
                        NotificationType.WARNING
                )
                .notify(this.project), ModalityState.nonModal());
    }
}
