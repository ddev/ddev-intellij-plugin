package de.php_perfect.intellij.ddev.terminal;

import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.jediterm.terminal.TtyConnector;
import de.php_perfect.intellij.ddev.actions.DdevStateStub;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.jetbrains.plugins.terminal.ShellStartupOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutionException;

final class PlatformTerminalRunnerTest extends BasePlatformTestCase {
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
    void sessionFailsWithoutDdev() throws NoSuchFieldException, IllegalAccessException {
        final Project project = getProject();
        final DdevTerminalRunner ddevTerminalRunner = new DdevTerminalRunner(project);

        final State state = DdevStateManager.getInstance(project).getState();
        final Field field = state.getClass().getDeclaredField("ddevBinary");
        field.setAccessible(true);
        field.set(state, null);

        final ShellStartupOptions.Builder builder = new ShellStartupOptions.Builder();
        builder.setWorkingDirectory(project.getBasePath());
        final ShellStartupOptions options = builder.build();

        Assertions.assertThrowsExactly(ExecutionException.class, () -> ddevTerminalRunner.createTtyConnector(options));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void connectorWrapsStartedDdevSession() throws ExecutionException {
        final State state = DdevStateStub.install(getProject(), getTestRootDisposable(), true, true, null).getState();
        Mockito.when(state.getDdevBinary()).thenReturn("/bin/echo");

        final TtyConnector connector = new DdevTerminalRunner(getProject()).createTtyConnector(new ShellStartupOptions.Builder().build());
        try {
            Assertions.assertEquals("Local", connector.getName());
        } finally {
            connector.close();
        }
    }
}
