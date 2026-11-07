package de.php_perfect.intellij.ddev.state;

import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

final class StateWatcherTest extends BasePlatformTestCase {

    private StateWatcherImpl stateWatcher;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        this.stateWatcher = new StateWatcherImpl(this.getProject());
    }

    @Test
    void startWatching() {
        stateWatcher.startWatching();
        Assertions.assertTrue(stateWatcher.isWatching());
    }

    @Test
    void startWatchingTwice() {
        stateWatcher.startWatching();
        stateWatcher.startWatching();
        Assertions.assertTrue(stateWatcher.isWatching());
    }

    @Test
    void startStopWatching() {
        stateWatcher.startWatching();
        stateWatcher.stopWatching();
        Assertions.assertFalse(stateWatcher.isWatching());
    }

    @Test
    void startStopWatchingWithoutStart() {
        stateWatcher.stopWatching();
        Assertions.assertFalse(stateWatcher.isWatching());
    }

    @Test
    void watcherUpdatesStatePeriodically() throws InterruptedException {
        final CountDownLatch updated = new CountDownLatch(1);
        final DdevStateManager stateManager = Mockito.mock(DdevStateManager.class);
        Mockito.doAnswer(invocation -> {
            updated.countDown();
            return null;
        }).when(stateManager).updateDescription();
        ServiceContainerUtil.replaceService(this.getProject(), DdevStateManager.class, stateManager, this.getTestRootDisposable());

        stateWatcher.startWatching();

        Assertions.assertTrue(updated.await(30, TimeUnit.SECONDS));
        stateWatcher.stopWatching();
        Mockito.verify(stateManager, Mockito.atLeastOnce()).updateConfiguration();
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        this.stateWatcher.dispose();
        super.tearDown();
    }
}
