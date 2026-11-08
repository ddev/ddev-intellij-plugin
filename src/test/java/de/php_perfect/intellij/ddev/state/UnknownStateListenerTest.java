package de.php_perfect.intellij.ddev.state;

import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.php_perfect.intellij.ddev.cmd.Description;
import de.php_perfect.intellij.ddev.notification.DdevNotifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

final class UnknownStateListenerTest extends BasePlatformTestCase {
    private static final Description RUNNING = Description.builder().status(Description.Status.RUNNING).build();

    private DdevNotifier notifier;
    private DdevStateManager stateManager;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        this.notifier = Mockito.mock(DdevNotifier.class);
        this.stateManager = Mockito.mock(DdevStateManager.class);
        ServiceContainerUtil.replaceService(this.getProject(), DdevNotifier.class, this.notifier, this.getTestRootDisposable());
        ServiceContainerUtil.replaceService(this.getProject(), DdevStateManager.class, this.stateManager, this.getTestRootDisposable());
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void unavailableDdevIsNotReported() {
        final UnknownStateListener listener = new UnknownStateListener(this.getProject());

        listener.onDdevChanged(state(false, true, null));
        listener.onDdevChanged(state(true, false, null));

        Mockito.verifyNoInteractions(this.stateManager, this.notifier);
    }

    @Test
    void knownStatusIsNotReported() {
        new UnknownStateListener(this.getProject()).onDdevChanged(state(true, true, RUNNING));

        Mockito.verifyNoInteractions(this.stateManager, this.notifier);
    }

    @Test
    void persistingUnknownStatusIsReportedAfterDelay() throws InterruptedException {
        final CountDownLatch checked = new CountDownLatch(3);
        final CountDownLatch notified = new CountDownLatch(1);
        final State stillUnknown = state(true, true, null);
        final State unavailable = state(false, true, null);
        final State running = state(true, true, RUNNING);
        Mockito.when(this.stateManager.getState())
                .thenAnswer(invocation -> countDown(checked, stillUnknown))
                .thenAnswer(invocation -> countDown(checked, unavailable))
                .thenAnswer(invocation -> countDown(checked, running));
        Mockito.doAnswer(invocation -> {
            notified.countDown();
            return null;
        }).when(this.notifier).notifyUnknownStateEntered();

        final State missingDescription = state(true, true, null);
        final State missingStatus = state(true, true, Description.builder().build());
        for (State unknownState : new State[]{missingDescription, missingStatus, missingDescription}) {
            final UnknownStateListener listener = new UnknownStateListener(this.getProject());
            listener.onDdevChanged(unknownState);
            listener.onDdevChanged(unknownState);
        }

        Assertions.assertTrue(checked.await(60, TimeUnit.SECONDS));
        Assertions.assertTrue(notified.await(10, TimeUnit.SECONDS));
        Mockito.verify(this.stateManager, Mockito.times(3)).getState();
        Mockito.verify(this.notifier).notifyUnknownStateEntered();
    }

    private static State countDown(CountDownLatch latch, State state) {
        latch.countDown();
        return state;
    }

    private static State state(boolean available, boolean configured, Description description) {
        final State state = Mockito.mock(State.class);
        Mockito.when(state.isAvailable()).thenReturn(available);
        Mockito.when(state.isConfigured()).thenReturn(configured);
        Mockito.when(state.getDescription()).thenReturn(description);
        return state;
    }
}
