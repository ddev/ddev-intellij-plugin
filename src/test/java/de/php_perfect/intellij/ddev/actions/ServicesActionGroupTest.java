package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.php_perfect.intellij.ddev.cmd.Description.Status;
import de.php_perfect.intellij.ddev.service_actions.ServiceActionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URI;

final class ServicesActionGroupTest extends BasePlatformTestCase {
    private final ServicesActionGroup group = new ServicesActionGroup();

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void childrenAreServiceActionsOfProject() {
        final AnAction[] serviceActions = {new OpenServiceAction(URI.create("https://test.ddev.site"), "Open", null, null)};
        final ServiceActionManager serviceActionManager = Mockito.mock(ServiceActionManager.class);
        Mockito.when(serviceActionManager.getServiceActions()).thenReturn(serviceActions);
        ServiceContainerUtil.replaceService(getProject(), ServiceActionManager.class, serviceActionManager, getTestRootDisposable());

        Assertions.assertArrayEquals(serviceActions, this.group.getChildren(projectEvent()));
    }

    @Test
    void groupWithoutEventHasNoChildren() {
        Assertions.assertEquals(0, this.group.getChildren(null).length);
    }

    @Test
    void groupWithoutProjectHasNoChildren() {
        Assertions.assertEquals(0, this.group.getChildren(TestActionEvent.createTestEvent(this.group, DataContext.EMPTY_CONTEXT)).length);
    }

    @Test
    void groupWithoutProjectIsDisabled() {
        final AnActionEvent event = TestActionEvent.createTestEvent(this.group, DataContext.EMPTY_CONTEXT);

        this.group.update(event);

        Assertions.assertFalse(event.getPresentation().isEnabled());
    }

    @Test
    void groupIsEnabledWhileProjectIsRunning() {
        DdevStateStub.install(getProject(), getTestRootDisposable(), true, true, Status.RUNNING);
        final AnActionEvent event = projectEvent();
        event.getPresentation().setEnabled(false);

        this.group.update(event);

        Assertions.assertTrue(event.getPresentation().isEnabled());
    }

    @Test
    void groupIsDisabledWhileProjectIsStopped() {
        DdevStateStub.install(getProject(), getTestRootDisposable(), true, true, Status.STOPPED);
        final AnActionEvent event = projectEvent();

        this.group.update(event);

        Assertions.assertFalse(event.getPresentation().isEnabled());
    }

    @Test
    void groupIsDisabledWithoutDescription() {
        DdevStateStub.install(getProject(), getTestRootDisposable(), true, true, null);
        final AnActionEvent event = projectEvent();

        this.group.update(event);

        Assertions.assertFalse(event.getPresentation().isEnabled());
    }

    @Test
    void groupUpdatesInBackground() {
        Assertions.assertEquals(ActionUpdateThread.BGT, this.group.getActionUpdateThread());
    }

    private AnActionEvent projectEvent() {
        return TestActionEvent.createTestEvent(this.group, SimpleDataContext.getProjectContext(getProject()));
    }
}
