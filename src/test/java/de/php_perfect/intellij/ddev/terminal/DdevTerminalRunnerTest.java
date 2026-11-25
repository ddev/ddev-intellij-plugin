package de.php_perfect.intellij.ddev.terminal;

import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.php_perfect.intellij.ddev.actions.DdevStateStub;
import de.php_perfect.intellij.ddev.state.DdevConfigLoader;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.MockDdevConfigLoader;
import de.php_perfect.intellij.ddev.state.State;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ExecutionException;

final class DdevTerminalRunnerTest extends BasePlatformTestCase {
    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Test
    void sessionCommandFailsWithoutDdev() throws NoSuchFieldException, IllegalAccessException {
        Project project = getProject();
        DdevTerminalRunner ddevTerminalRunner = new DdevTerminalRunner(project);

        State state = DdevStateManager.getInstance(project).getState();

        Field field = state.getClass().getDeclaredField("ddevBinary");
        field.setAccessible(true);
        field.set(state, null);

        Assertions.assertThrowsExactly(ExecutionException.class, ddevTerminalRunner::createSessionCommand);
    }

    @Test
    void sessionCommandRunsDdevSsh() throws ExecutionException {
        stubDdevBinary("/usr/local/bin/ddev");

        Assertions.assertEquals(List.of("/usr/local/bin/ddev", "ssh"), new DdevTerminalRunner(getProject()).createSessionCommand());
    }

    @Test
    void defaultTabTitleNamesWebContainer() {
        Assertions.assertEquals("DDEV Web Container", new DdevTerminalRunner(getProject()).getDefaultTabTitle());
    }

    @Test
    void terminalIsNotPersistent() {
        Project project = getProject();
        DdevTerminalRunner ddevTerminalRunner = new DdevTerminalRunner(project);

        Assertions.assertFalse(ddevTerminalRunner.isTerminalSessionPersistent());
    }

    private void stubDdevBinary(String binary) {
        final State state = DdevStateStub.install(getProject(), getTestRootDisposable(), true, true, null).getState();
        Mockito.when(state.getDdevBinary()).thenReturn(binary);
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        final MockDdevConfigLoader ddevConfigLoader = (MockDdevConfigLoader) DdevConfigLoader.getInstance(this.getProject());
        ddevConfigLoader.setExists(false);

        DdevStateManager.getInstance(this.getProject()).resetState();

        super.tearDown();
    }
}
