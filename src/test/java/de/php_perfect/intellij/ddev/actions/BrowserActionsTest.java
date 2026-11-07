package de.php_perfect.intellij.ddev.actions;

import com.intellij.ide.browsers.BrowserLauncher;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class BrowserActionsTest extends BasePlatformTestCase {
    private BrowserLauncher browserLauncher;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        this.browserLauncher = Mockito.mock(BrowserLauncher.class);
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), BrowserLauncher.class, this.browserLauncher, getTestRootDisposable());
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void installationInstructionsOpenDdevDocumentation() {
        perform(new InstallationInstructionsAction());

        Mockito.verify(this.browserLauncher).browse(Mockito.eq("https://ddev.readthedocs.io/en/stable/"), Mockito.any(), Mockito.any());
    }

    @Test
    void reportIssueOpensBugReportForm() {
        perform(new ReportIssueAction());

        Mockito.verify(this.browserLauncher).browse(
                Mockito.eq("https://github.com/ddev/ddev-intellij-plugin/issues/new?assignees=&labels=bug&template=bug_report.yml"),
                Mockito.any(), Mockito.any());
    }

    private void perform(AnAction action) {
        action.actionPerformed(TestActionEvent.createTestEvent(action, SimpleDataContext.getProjectContext(getProject())));
    }
}
