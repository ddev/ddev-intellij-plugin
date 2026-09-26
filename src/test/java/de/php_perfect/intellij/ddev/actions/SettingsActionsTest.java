package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.php_perfect.intellij.ddev.settings.DdevSettingsState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class SettingsActionsTest extends BasePlatformTestCase {
    private ShowSettingsUtil showSettingsUtil;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        this.showSettingsUtil = Mockito.mock(ShowSettingsUtil.class);
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), ShowSettingsUtil.class, this.showSettingsUtil, getTestRootDisposable());
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        DdevSettingsState.getInstance(getProject()).checkForUpdates = true;
        super.tearDown();
    }

    @Test
    void managePluginsOpensPluginSettings() {
        performWithProject(new ManagePluginsAction());

        Mockito.verify(this.showSettingsUtil).showSettingsDialog(getProject(), "Plugins");
    }

    @Test
    void managePluginsWithoutProjectOpensNothing() {
        performWithoutProject(new ManagePluginsAction());

        Mockito.verifyNoInteractions(this.showSettingsUtil);
    }

    @Test
    void disableCheckForUpdatesTurnsOffUpdateCheck() {
        final DdevSettingsState settings = DdevSettingsState.getInstance(getProject());
        settings.checkForUpdates = true;

        performWithProject(new DisableCheckForUpdatesAction());

        Assertions.assertFalse(settings.checkForUpdates);
    }

    @Test
    void disableCheckForUpdatesWithoutProjectKeepsSetting() {
        final DdevSettingsState settings = DdevSettingsState.getInstance(getProject());
        settings.checkForUpdates = true;

        performWithoutProject(new DisableCheckForUpdatesAction());

        Assertions.assertTrue(settings.checkForUpdates);
    }

    private void performWithProject(AnAction action) {
        action.actionPerformed(TestActionEvent.createTestEvent(action, SimpleDataContext.getProjectContext(getProject())));
    }

    private void performWithoutProject(AnAction action) {
        action.actionPerformed(TestActionEvent.createTestEvent(action, DataContext.EMPTY_CONTEXT));
    }
}
