package de.php_perfect.intellij.ddev.error_reporting;

import com.intellij.execution.wsl.WSLDistribution;
import com.intellij.execution.wsl.WslPath;
import com.intellij.ide.DataManager;
import com.intellij.idea.IdeaLogger;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.CommandFailedException;
import de.php_perfect.intellij.ddev.cmd.Ddev;
import de.php_perfect.intellij.ddev.cmd.Versions;
import de.php_perfect.intellij.ddev.notification.DdevNotifier;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.protocol.SentryId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class SentryErrorReporter extends ErrorReportSubmitter {
    @NotNull
    @Override
    public String getReportActionText() {
        return DdevIntegrationBundle.message("errorReporting.submit");
    }

    @Override
    public @Nullable String getPrivacyNoticeText() {
        return DdevIntegrationBundle.message("errorReporting.privacyNotice");
    }

    @Override
    public boolean submit(IdeaLoggingEvent @NotNull [] events, @Nullable String additionalInfo, @NotNull Component parentComponent, @NotNull Consumer<? super SubmittedReportInfo> consumer) {
        DataContext context = DataManager.getInstance().getDataContext(parentComponent);
        Project project = CommonDataKeys.PROJECT.getData(context);

        ProgressManager.getInstance().run(new Task.Backgroundable(project, DdevIntegrationBundle.message("errorReporting.taskTitle")) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                Versions ddevVersions = getDdevVersions(project);
                WSLDistribution wslDistribution = getWslDistribution(project);

                for (IdeaLoggingEvent event : events) {
                    SentryId sentryId = captureIdeaLoggingEvent(event, additionalInfo, ddevVersions, wslDistribution);

                    if (project != null) {
                        DdevNotifier.getInstance(project).notifyErrorReportSent(sentryId.toString());
                    }
                }

                consumer.consume(new SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE));
            }
        });

        return true;
    }

    private SentryId captureIdeaLoggingEvent(IdeaLoggingEvent event, @Nullable String additionalInfo, @Nullable Versions ddevVersions, @Nullable WSLDistribution wslDistribution) {
        return Sentry.captureEvent(buildSentryEvent(event, additionalInfo, ddevVersions, wslDistribution));
    }

    private SentryEvent buildSentryEvent(IdeaLoggingEvent ideaLoggingEvent, @Nullable String additionalInfo, @Nullable Versions ddevVersions, @Nullable WSLDistribution wslDistribution) {
        SentryEvent event = new SentryEvent();
        event.setRelease(getPluginDescriptor().getVersion());

        if (additionalInfo != null) {
            event.setExtra("additional_info", additionalInfo);
        }

        event.setThrowable(ideaLoggingEvent.getThrowable());
        if (ddevVersions != null) {
            event.setTag("ddev_version", ddevVersions.getDdevVersion() != null ? ddevVersions.getDdevVersion() : "");
            event.setTag("docker_version", ddevVersions.getDockerVersion() != null ? ddevVersions.getDockerVersion() : "");
            event.setTag("docker_platform", ddevVersions.getDockerPlatform() != null ? ddevVersions.getDockerPlatform() : "");
            event.setTag("docker_compose_version", ddevVersions.getDockerComposeVersion() != null ? ddevVersions.getDockerComposeVersion() : "");
        }

        if (wslDistribution != null) {
            event.setTag("wsl_distribution", wslDistribution.getMsId());
        }

        event.setExtra("last_action_id", IdeaLogger.ourLastActionId);

        return event;
    }

    private @Nullable Versions getDdevVersions(@Nullable Project project) {
        if (project == null) {
            return null;
        }

        final String binary = DdevStateManager.getInstance(project).getState().getDdevBinary();

        if (binary == null) {
            return null;
        }

        try {
            return Ddev.getInstance().detailedVersions(binary, project);
        } catch (CommandFailedException e) {
            return null;
        }
    }

    private @Nullable WSLDistribution getWslDistribution(@Nullable Project project) {
        if (project == null || project.getBasePath() == null) {
            return null;
        }

        return WslPath.getDistributionByWindowsUncPath(project.getBasePath());
    }
}
