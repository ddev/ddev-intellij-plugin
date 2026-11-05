package de.php_perfect.intellij.ddev.terminal;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.php_perfect.intellij.ddev.actions.DdevStateStub;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

final class DdevPredefinedTerminalActionProviderTest extends BasePlatformTestCase {
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
    void listOpenPredefinedTerminalActions() {
        Project project = getProject();
        DdevPredefinedTerminalActionProvider ddevPredefinedTerminalActionProvider = new DdevPredefinedTerminalActionProvider();

        Assertions.assertTrue(ddevPredefinedTerminalActionProvider.listOpenPredefinedTerminalActions(project).isEmpty());
    }

    @Test
    void configuredProjectOffersDdevTerminal() {
        DdevStateStub.install(getProject(), getTestRootDisposable(), true, true, null);

        Assertions.assertEquals(
                List.of(ActionManager.getInstance().getAction("DdevIntegration.Terminal")),
                new DdevPredefinedTerminalActionProvider().listOpenPredefinedTerminalActions(getProject()));
    }

    @Test
    void unconfiguredProjectOffersNoTerminal() {
        DdevStateStub.install(getProject(), getTestRootDisposable(), true, false, null);

        Assertions.assertTrue(new DdevPredefinedTerminalActionProvider().listOpenPredefinedTerminalActions(getProject()).isEmpty());
    }
}
