package de.php_perfect.intellij.ddev.notification;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationsManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class DdevNotifierTest extends BasePlatformTestCase {
    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Test
    void notifyInstallDdev() {
        Project project = getProject();

        NotificationsManager notificationManager = NotificationsManager.getNotificationsManager();
        Notification[] notifications = notificationManager.getNotificationsOfType(Notification.class, project);
        assertEmpty(notifications);

        new DdevNotifierImpl(project).notifyInstallDdev();

        this.waitForEventQueue();

        notifications = notificationManager.getNotificationsOfType(Notification.class, project);
        assertSize(1, notifications);
    }

    @Test
    void notifyNewVersionAvailable() {
        Project project = getProject();

        NotificationsManager notificationManager = NotificationsManager.getNotificationsManager();
        Notification[] notifications = notificationManager.getNotificationsOfType(Notification.class, project);
        assertEmpty(notifications);

        new DdevNotifierImpl(project).notifyNewVersionAvailable("1.0.0", "2.0.0");

        this.waitForEventQueue();

        notifications = notificationManager.getNotificationsOfType(Notification.class, project);
        assertSize(1, notifications);
    }

    @Test
    void notifyAlreadyLatestVersion() {
        Project project = getProject();

        NotificationsManager notificationManager = NotificationsManager.getNotificationsManager();
        Notification[] notifications = notificationManager.getNotificationsOfType(Notification.class, project);
        assertEmpty(notifications);

        new DdevNotifierImpl(project).notifyAlreadyLatestVersion();

        this.waitForEventQueue();

        notifications = notificationManager.getNotificationsOfType(Notification.class, project);
        assertSize(1, notifications);
    }

    @Test
    void notifyMissingPlugin() {
        Project project = getProject();

        NotificationsManager notificationManager = NotificationsManager.getNotificationsManager();
        Notification[] notifications = notificationManager.getNotificationsOfType(Notification.class, project);
        assertEmpty(notifications);

        new DdevNotifierImpl(project).notifyMissingPlugin("Some Plugin", "Test Feature");

        this.waitForEventQueue();

        notifications = notificationManager.getNotificationsOfType(Notification.class, project);
        assertSize(1, notifications);
    }

    @Test
    void notifyPhpInterpreterUpdated() {
        Project project = getProject();

        NotificationsManager notificationManager = NotificationsManager.getNotificationsManager();
        Notification[] notifications = notificationManager.getNotificationsOfType(Notification.class, project);
        assertEmpty(notifications);

        new DdevNotifierImpl(project).notifyPhpInterpreterUpdated("php99.9");

        this.waitForEventQueue();

        notifications = notificationManager.getNotificationsOfType(Notification.class, project);
        assertSize(1, notifications);
    }

    @Test
    void notifyUnknownStateEntered() {
        Project project = getProject();

        NotificationsManager notificationManager = NotificationsManager.getNotificationsManager();
        Notification[] notifications = notificationManager.getNotificationsOfType(Notification.class, project);
        assertEmpty(notifications);

        new DdevNotifierImpl(project).notifyUnknownStateEntered();

        this.waitForEventQueue();

        notifications = notificationManager.getNotificationsOfType(Notification.class, project);
        assertSize(1, notifications);
    }

    @Test
    void notifyDdevDetected() {
        Project project = getProject();

        NotificationsManager notificationManager = NotificationsManager.getNotificationsManager();
        Notification[] notifications = notificationManager.getNotificationsOfType(Notification.class, project);
        assertEmpty(notifications);

        new DdevNotifierImpl(project).notifyDdevDetected("/some/path/ddev");

        this.waitForEventQueue();

        notifications = notificationManager.getNotificationsOfType(Notification.class, project);
        assertSize(1, notifications);
    }

    @Test
    void notifyErrorReportSent() {
        Project project = getProject();

        NotificationsManager notificationManager = NotificationsManager.getNotificationsManager();
        Notification[] notifications = notificationManager.getNotificationsOfType(Notification.class, project);
        assertEmpty(notifications);

        new DdevNotifierImpl(project).notifyErrorReportSent("cc83481fd7b74744afdd7f36ba827f7b");

        this.waitForEventQueue();

        notifications = notificationManager.getNotificationsOfType(Notification.class, project);
        assertSize(1, notifications);
        assertTrue(notifications[0].getContent().contains("cc83481fd7b74744afdd7f36ba827f7b"));
    }

    @Test
    void notifyDockerNotAvailable() {
        Project project = getProject();

        NotificationsManager notificationManager = NotificationsManager.getNotificationsManager();
        Notification[] notifications = notificationManager.getNotificationsOfType(Notification.class, project);
        assertEmpty(notifications);

        new DdevNotifierImpl(project).notifyDockerNotAvailable("default");

        this.waitForEventQueue();

        notifications = notificationManager.getNotificationsOfType(Notification.class, project);
        assertSize(1, notifications);
    }

    private void waitForEventQueue() {
        ApplicationManager.getApplication().invokeAndWait(PlatformTestUtil::dispatchAllInvocationEventsInIdeEventQueue);
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        NotificationsManager notificationManager = NotificationsManager.getNotificationsManager();
        Notification[] notifications = notificationManager.getNotificationsOfType(Notification.class, getProject());
        for (Notification notification : notifications) {
            notificationManager.expire(notification);
        }

        super.tearDown();
    }
}
