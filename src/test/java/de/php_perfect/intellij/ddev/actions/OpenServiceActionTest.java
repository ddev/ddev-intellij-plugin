package de.php_perfect.intellij.ddev.actions;

import com.intellij.ide.browsers.BrowserLauncher;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URI;

final class OpenServiceActionTest extends BasePlatformTestCase {
    private static final URI URI_WEB = URI.create("https://test.ddev.site");

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
    void actionOpensServiceUriInBrowser() {
        final BrowserLauncher browserLauncher = Mockito.mock(BrowserLauncher.class);
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), BrowserLauncher.class, browserLauncher, getTestRootDisposable());
        final OpenServiceAction action = new OpenServiceAction(URI_WEB, "Open", "Open the web service", null);

        action.actionPerformed(TestActionEvent.createTestEvent(action, SimpleDataContext.getProjectContext(getProject())));

        Mockito.verify(browserLauncher).browse(URI_WEB);
    }

    @Test
    void actionsWithSameUriAreEqual() {
        final OpenServiceAction action = new OpenServiceAction(URI_WEB, "Open", null, null);
        final OpenServiceAction sameUri = new OpenServiceAction(URI_WEB, "Other text", "Other description", null);

        Assertions.assertEquals(action, action);
        Assertions.assertEquals(action, sameUri);
        Assertions.assertEquals(action.hashCode(), sameUri.hashCode());
    }

    @Test
    void actionsWithDifferentUriAreNotEqual() {
        final OpenServiceAction action = new OpenServiceAction(URI_WEB, "Open", null, null);
        final OpenServiceAction otherUri = new OpenServiceAction(URI.create("https://mail.ddev.site"), "Open", null, null);

        Assertions.assertNotEquals(action, otherUri);
        Assertions.assertNotEquals(action, null);
        Assertions.assertNotEquals(action, new DdevStartAction());
    }

    @Test
    void actionUpdatesInBackground() {
        Assertions.assertEquals(ActionUpdateThread.BGT, new OpenServiceAction(URI_WEB, "Open", null, null).getActionUpdateThread());
    }
}
