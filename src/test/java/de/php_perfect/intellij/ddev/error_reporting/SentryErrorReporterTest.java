package de.php_perfect.intellij.ddev.error_reporting;

import com.intellij.execution.wsl.WSLDistribution;
import com.intellij.execution.wsl.WslPath;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.Consumer;
import de.php_perfect.intellij.ddev.cmd.CommandFailedException;
import de.php_perfect.intellij.ddev.cmd.Ddev;
import de.php_perfect.intellij.ddev.cmd.Versions;
import de.php_perfect.intellij.ddev.notification.DdevNotifier;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.protocol.SentryId;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.swing.JPanel;
import java.util.List;

final class SentryErrorReporterTest extends BasePlatformTestCase {
    private static final SentryId SENTRY_ID = new SentryId("0123456789abcdef0123456789abcdef");

    private DataManager dataManager;
    private DdevNotifier notifier;
    private State state;
    private Ddev ddev;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();

        this.dataManager = Mockito.mock(DataManager.class);
        this.notifier = Mockito.mock(DdevNotifier.class);
        this.state = Mockito.mock(State.class);
        this.ddev = Mockito.mock(Ddev.class);

        final DdevStateManager stateManager = Mockito.mock(DdevStateManager.class);
        Mockito.when(stateManager.getState()).thenReturn(this.state);

        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), DataManager.class, this.dataManager, getTestRootDisposable());
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), Ddev.class, this.ddev, getTestRootDisposable());
        ServiceContainerUtil.replaceService(getProject(), DdevNotifier.class, this.notifier, getTestRootDisposable());
        ServiceContainerUtil.replaceService(getProject(), DdevStateManager.class, stateManager, getTestRootDisposable());
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void texts() {
        final SentryErrorReporter reporter = new SentryErrorReporter();

        Assertions.assertEquals("Report to Author", reporter.getReportActionText());
        Assertions.assertTrue(reporter.getPrivacyNoticeText().startsWith("I agree"));
    }

    @Test
    void reportsEventsWithoutProject() {
        final RuntimeException first = new RuntimeException("first");
        final RuntimeException second = new RuntimeException("second");

        final List<SentryEvent> events = this.submit(DataContext.EMPTY_CONTEXT, null, null, first, second);

        Assertions.assertEquals(2, events.size());
        Assertions.assertSame(first, events.get(0).getThrowable());
        Assertions.assertSame(second, events.get(1).getThrowable());
        Assertions.assertEquals(PluginVersion.get(), events.get(0).getRelease());
        Assertions.assertNull(events.get(0).getExtra("additional_info"));
        Assertions.assertNull(events.get(0).getTag("ddev_version"));
        Assertions.assertNull(events.get(0).getTag("wsl_distribution"));
        Mockito.verifyNoInteractions(this.notifier, this.ddev);
    }

    @Test
    void reportsEventWithDdevVersionsAndWslDistribution() throws CommandFailedException {
        Mockito.when(this.state.getDdevBinary()).thenReturn("ddev");
        Mockito.when(this.ddev.detailedVersions("ddev", getProject())).thenReturn(new Versions("v1.23.0", "24.0.7", "v2.23.3", "docker-desktop"));
        final WSLDistribution distribution = Mockito.mock(WSLDistribution.class);
        Mockito.when(distribution.getMsId()).thenReturn("Ubuntu");

        final SentryEvent event = this.submit(projectContext(getProject()), "clicked start", distribution, new RuntimeException()).get(0);

        Assertions.assertEquals("clicked start", event.getExtra("additional_info"));
        Assertions.assertEquals("v1.23.0", event.getTag("ddev_version"));
        Assertions.assertEquals("24.0.7", event.getTag("docker_version"));
        Assertions.assertEquals("docker-desktop", event.getTag("docker_platform"));
        Assertions.assertEquals("v2.23.3", event.getTag("docker_compose_version"));
        Assertions.assertEquals("Ubuntu", event.getTag("wsl_distribution"));
        Mockito.verify(this.notifier).notifyErrorReportSent(SENTRY_ID.toString());
    }

    @Test
    void unknownVersionsAreReportedAsEmptyTags() throws CommandFailedException {
        Mockito.when(this.state.getDdevBinary()).thenReturn("ddev");
        Mockito.when(this.ddev.detailedVersions("ddev", getProject())).thenReturn(new Versions(null, null, null, null));

        final SentryEvent event = this.submit(projectContext(getProject()), null, null, new RuntimeException()).get(0);

        Assertions.assertEquals("", event.getTag("ddev_version"));
        Assertions.assertEquals("", event.getTag("docker_version"));
        Assertions.assertEquals("", event.getTag("docker_platform"));
        Assertions.assertEquals("", event.getTag("docker_compose_version"));
        Assertions.assertNull(event.getTag("wsl_distribution"));
    }

    @Test
    void failingVersionCommandIsSkipped() throws CommandFailedException {
        Mockito.when(this.state.getDdevBinary()).thenReturn("ddev");
        Mockito.when(this.ddev.detailedVersions("ddev", getProject())).thenThrow(new CommandFailedException("Docker is not running"));

        final SentryEvent event = this.submit(projectContext(getProject()), null, null, new RuntimeException()).get(0);

        Assertions.assertNull(event.getTag("ddev_version"));
        Mockito.verify(this.notifier).notifyErrorReportSent(SENTRY_ID.toString());
    }

    @Test
    void missingBinarySkipsVersions() {
        Mockito.when(this.state.getDdevBinary()).thenReturn(null);

        final SentryEvent event = this.submit(projectContext(getProject()), null, null, new RuntimeException()).get(0);

        Assertions.assertNull(event.getTag("ddev_version"));
        Mockito.verifyNoInteractions(this.ddev);
    }

    @Test
    void projectWithoutBasePathSkipsWslLookup() {
        final Project project = Mockito.mock(Project.class);
        final DdevStateManager stateManager = Mockito.mock(DdevStateManager.class);
        Mockito.when(stateManager.getState()).thenReturn(this.state);
        Mockito.when(project.getService(DdevStateManager.class)).thenReturn(stateManager);
        Mockito.when(project.getService(DdevNotifier.class)).thenReturn(this.notifier);
        Mockito.when(project.getBasePath()).thenReturn(null);

        final WSLDistribution distribution = Mockito.mock(WSLDistribution.class);
        Mockito.when(distribution.getMsId()).thenReturn("Ubuntu");

        final SentryEvent event = this.submit(projectContext(project), null, distribution, new RuntimeException()).get(0);

        Assertions.assertNull(event.getTag("wsl_distribution"));
        Mockito.verify(this.notifier).notifyErrorReportSent(SENTRY_ID.toString());
    }

    private static DataContext projectContext(Project project) {
        final DataContext context = Mockito.mock(DataContext.class);
        Mockito.when(context.getData(CommonDataKeys.PROJECT)).thenReturn(project);
        return context;
    }

    @SuppressWarnings("unchecked")
    private List<SentryEvent> submit(DataContext context, @Nullable String additionalInfo, @Nullable WSLDistribution distribution, Throwable... throwables) {
        final JPanel parent = new JPanel();
        Mockito.when(this.dataManager.getDataContext(parent)).thenReturn(context);

        final IdeaLoggingEvent[] events = new IdeaLoggingEvent[throwables.length];
        for (int i = 0; i < throwables.length; i++) {
            events[i] = new IdeaLoggingEvent(throwables[i].getMessage(), throwables[i]);
        }

        final Consumer<SubmittedReportInfo> consumer = Mockito.mock(Consumer.class);
        final ArgumentCaptor<SentryEvent> captured = ArgumentCaptor.forClass(SentryEvent.class);
        final ArgumentCaptor<SubmittedReportInfo> reportInfo = ArgumentCaptor.forClass(SubmittedReportInfo.class);

        try (MockedStatic<Sentry> sentry = Mockito.mockStatic(Sentry.class);
             MockedStatic<WslPath> wslPath = Mockito.mockStatic(WslPath.class)) {
            sentry.when(() -> Sentry.captureEvent(Mockito.any(SentryEvent.class))).thenReturn(SENTRY_ID);
            wslPath.when(() -> WslPath.getDistributionByWindowsUncPath(Mockito.anyString())).thenReturn(distribution);

            Assertions.assertTrue(new SentryErrorReporter().submit(events, additionalInfo, parent, consumer));

            sentry.verify(() -> Sentry.captureEvent(captured.capture()), Mockito.times(throwables.length));
        }

        Mockito.verify(consumer).consume(reportInfo.capture());
        Assertions.assertEquals(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE, reportInfo.getValue().getStatus());

        return captured.getAllValues();
    }
}
