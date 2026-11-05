package de.php_perfect.intellij.ddev.terminal;

import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.jetbrains.plugins.terminal.ShellStartupOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

        Assertions.assertThrowsExactly(ExecutionException.class, () -> ddevTerminalRunner.createProcess(options));
    }
}
