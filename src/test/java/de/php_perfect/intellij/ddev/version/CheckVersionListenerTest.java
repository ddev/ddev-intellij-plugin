package de.php_perfect.intellij.ddev.version;

import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.php_perfect.intellij.ddev.settings.DdevSettingsState;
import de.php_perfect.intellij.ddev.state.State;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class CheckVersionListenerTest extends BasePlatformTestCase {
    private VersionChecker versionChecker;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();

        this.versionChecker = Mockito.mock(VersionChecker.class);
        ServiceContainerUtil.replaceService(getProject(), VersionChecker.class, this.versionChecker, getTestRootDisposable());
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        DdevSettingsState.getInstance(getProject()).checkForUpdates = true;
        super.tearDown();
    }

    @Test
    void checksVersionWhenUpdatesAreEnabled() {
        DdevSettingsState.getInstance(getProject()).checkForUpdates = true;

        new CheckVersionListener(getProject()).onStateInitialized(Mockito.mock(State.class));

        Mockito.verify(this.versionChecker).checkDdevVersion();
    }

    @Test
    void skipsVersionCheckWhenUpdatesAreDisabled() {
        DdevSettingsState.getInstance(getProject()).checkForUpdates = false;

        new CheckVersionListener(getProject()).onStateInitialized(Mockito.mock(State.class));

        Mockito.verifyNoInteractions(this.versionChecker);
    }
}
