package de.php_perfect.intellij.ddev.version;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.php_perfect.intellij.ddev.notification.DdevNotifier;
import de.php_perfect.intellij.ddev.settings.DdevSettingsState;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class VersionCheckerImplTest extends BasePlatformTestCase {
    private DdevNotifier notifier;
    private ReleaseClient releaseClient;
    private State state;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();

        this.notifier = Mockito.mock(DdevNotifier.class);
        this.releaseClient = Mockito.mock(ReleaseClient.class);
        this.state = Mockito.mock(State.class);

        final DdevStateManager stateManager = Mockito.mock(DdevStateManager.class);
        Mockito.when(stateManager.getState()).thenReturn(this.state);

        ServiceContainerUtil.replaceService(getProject(), DdevNotifier.class, this.notifier, getTestRootDisposable());
        ServiceContainerUtil.replaceService(getProject(), DdevStateManager.class, stateManager, getTestRootDisposable());
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), ReleaseClient.class, this.releaseClient, getTestRootDisposable());
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        DdevSettingsState.getInstance(getProject()).checkForUpdates = true;
        super.tearDown();
    }

    @Test
    void disabledUpdateCheckDoesNothing() {
        DdevSettingsState.getInstance(getProject()).checkForUpdates = false;

        new VersionCheckerImpl(getProject()).checkDdevVersion();

        Mockito.verifyNoInteractions(this.notifier, this.releaseClient, this.state);
    }

    @Test
    void unavailableConfiguredDdevAsksForInstallation() {
        Mockito.when(this.state.isAvailable()).thenReturn(false);
        Mockito.when(this.state.isConfigured()).thenReturn(true);

        new VersionCheckerImpl(getProject()).checkDdevVersion();

        Mockito.verify(this.notifier).notifyInstallDdev();
        Mockito.verifyNoInteractions(this.releaseClient);
    }

    @Test
    void unavailableUnconfiguredDdevStaysQuiet() {
        Mockito.when(this.state.isAvailable()).thenReturn(false);
        Mockito.when(this.state.isConfigured()).thenReturn(false);

        new VersionCheckerImpl(getProject()).checkDdevVersion();

        Mockito.verifyNoInteractions(this.notifier, this.releaseClient);
    }

    @Test
    void missingVersionOfAvailableDdevStaysQuiet() {
        Mockito.when(this.state.isAvailable()).thenReturn(true);
        Mockito.when(this.state.getDdevVersion()).thenReturn(null);
        Mockito.when(this.state.isConfigured()).thenReturn(false);

        new VersionCheckerImpl(getProject()).checkDdevVersion();

        Mockito.verifyNoInteractions(this.notifier, this.releaseClient);
    }

    @Test
    void newerReleaseIsAnnounced() {
        this.prepareRelease("v1.22.0", new LatestRelease("v1.23.0"));

        new VersionCheckerImpl(getProject()).checkDdevVersion();

        Mockito.verify(this.notifier).notifyNewVersionAvailable("v1.22.0", "v1.23.0");
    }

    @Test
    void latestVersionIsConfirmedOnRequest() {
        this.prepareRelease("v1.23.0", new LatestRelease("v1.23.0"));

        new VersionCheckerImpl(getProject()).checkDdevVersion(true);

        Mockito.verify(this.notifier).notifyAlreadyLatestVersion();
    }

    @Test
    void latestVersionIsNotConfirmedByDefault() {
        this.prepareRelease("v1.23.0", new LatestRelease("v1.23.0"));

        new VersionCheckerImpl(getProject()).checkDdevVersion();

        Mockito.verify(this.releaseClient).loadCurrentVersion(Mockito.any(ProgressIndicator.class));
        Mockito.verifyNoInteractions(this.notifier);
    }

    @Test
    void missingReleaseIsIgnored() {
        this.prepareRelease("v1.23.0", null);

        new VersionCheckerImpl(getProject()).checkDdevVersion(true);

        Mockito.verify(this.releaseClient).loadCurrentVersion(Mockito.any(ProgressIndicator.class));
        Mockito.verifyNoInteractions(this.notifier);
    }

    @Test
    void releaseWithoutTagIsIgnored() {
        this.prepareRelease("v1.23.0", new LatestRelease(null));

        new VersionCheckerImpl(getProject()).checkDdevVersion(true);

        Mockito.verify(this.releaseClient).loadCurrentVersion(Mockito.any(ProgressIndicator.class));
        Mockito.verifyNoInteractions(this.notifier);
    }

    private void prepareRelease(String currentVersion, LatestRelease latestRelease) {
        Mockito.when(this.state.isAvailable()).thenReturn(true);
        Mockito.when(this.state.getDdevVersion()).thenReturn(new Version(currentVersion));
        Mockito.when(this.releaseClient.loadCurrentVersion(Mockito.any(ProgressIndicator.class))).thenReturn(latestRelease);
    }
}
