package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.php_perfect.intellij.ddev.terminal.DdevTerminalRunner;
import org.jetbrains.plugins.terminal.TerminalTabState;
import org.jetbrains.plugins.terminal.TerminalToolWindowManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class DdevPredefinedTerminalActionTest extends BasePlatformTestCase {
    private TerminalToolWindowManager terminalManager;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        this.terminalManager = Mockito.mock(TerminalToolWindowManager.class);
        ServiceContainerUtil.replaceService(getProject(), TerminalToolWindowManager.class, this.terminalManager, getTestRootDisposable());
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void actionOpensDdevTerminalSession() {
        final DdevPredefinedTerminalAction action = new DdevPredefinedTerminalAction();

        action.actionPerformed(TestActionEvent.createTestEvent(action, SimpleDataContext.getProjectContext(getProject())));

        Mockito.verify(this.terminalManager).createNewSession(
                Mockito.any(DdevTerminalRunner.class),
                Mockito.argThat((TerminalTabState state) -> "DDEV Web Container".equals(state.myTabName)));
    }

    @Test
    void actionWithoutProjectOpensNothing() {
        final DdevPredefinedTerminalAction action = new DdevPredefinedTerminalAction();

        action.actionPerformed(TestActionEvent.createTestEvent(action, DataContext.EMPTY_CONTEXT));

        Mockito.verifyNoInteractions(this.terminalManager);
    }

    @Test
    void actionUpdatesInBackground() {
        Assertions.assertEquals(ActionUpdateThread.BGT, new DdevPredefinedTerminalAction().getActionUpdateThread());
    }
}
