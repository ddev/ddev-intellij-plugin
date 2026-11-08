package de.php_perfect.intellij.ddev.state;

import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.php_perfect.intellij.ddev.settings.DdevSettingsState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class StartWatcherListenerTest extends BasePlatformTestCase {
    private StateWatcher stateWatcher;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        this.stateWatcher = Mockito.mock(StateWatcher.class);
        ServiceContainerUtil.replaceService(this.getProject(), StateWatcher.class, this.stateWatcher, this.getTestRootDisposable());
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        DdevSettingsState.getInstance(this.getProject()).watchDdev = true;
        super.tearDown();
    }

    @Test
    void startsWatcherOnInitialization() {
        new StartWatcherListener(this.getProject()).onStateInitialized(new StateImpl());

        Mockito.verify(this.stateWatcher).startWatching();
    }

    @Test
    void disabledWatchingIsRespected() {
        DdevSettingsState.getInstance(this.getProject()).watchDdev = false;

        new StartWatcherListener(this.getProject()).onStateInitialized(new StateImpl());

        Mockito.verifyNoInteractions(this.stateWatcher);
    }
}
