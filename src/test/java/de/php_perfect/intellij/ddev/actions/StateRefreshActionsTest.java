package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

final class StateRefreshActionsTest extends BasePlatformTestCase {
    private DdevStateManager stateManager;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        this.stateManager = DdevStateStub.install(getProject(), getTestRootDisposable(), Mockito.mock(DdevStateManager.class));
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void syncStateUpdatesDescription() throws InterruptedException {
        final CountDownLatch updated = new CountDownLatch(1);
        Mockito.doAnswer(invocation -> {
            updated.countDown();
            return null;
        }).when(this.stateManager).updateDescription();

        performWithProject(new SyncStateAction());

        Assertions.assertTrue(updated.await(5, TimeUnit.SECONDS));
    }

    @Test
    void reloadPluginInitializesState() throws InterruptedException {
        final CountDownLatch initialized = new CountDownLatch(1);
        Mockito.doAnswer(invocation -> {
            initialized.countDown();
            return null;
        }).when(this.stateManager).initialize();

        performWithProject(new ReloadPluginAction());

        Assertions.assertTrue(initialized.await(5, TimeUnit.SECONDS));
    }

    @Test
    void actionsWithoutProjectLeaveStateAlone() {
        performWithoutProject(new SyncStateAction());
        performWithoutProject(new ReloadPluginAction());

        Mockito.verifyNoInteractions(this.stateManager);
    }

    private void performWithProject(AnAction action) {
        action.actionPerformed(TestActionEvent.createTestEvent(action, SimpleDataContext.getProjectContext(getProject())));
    }

    private void performWithoutProject(AnAction action) {
        action.actionPerformed(TestActionEvent.createTestEvent(action, DataContext.EMPTY_CONTEXT));
    }
}
