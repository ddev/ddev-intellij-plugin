package de.php_perfect.intellij.ddev.terminal;

import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.jediterm.core.util.TermSize;
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

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
    void terminalPluginRunsDdevSsh() throws Exception {
        final State state = DdevStateStub.install(getProject(), getTestRootDisposable(), true, true, null).getState();
        Mockito.when(state.getDdevBinary()).thenReturn("/bin/echo");
        Files.createDirectories(Path.of(Objects.requireNonNull(getProject().getBasePath())));

        final ShellStartupOptions.Builder options = new ShellStartupOptions.Builder();
        options.setInitialTermSize(new TermSize(80, 24));

        final TtyConnector connector = new DdevTerminalRunner(getProject()).createTtyConnector(options.build());
        try {
            final CompletableFuture<String> output = CompletableFuture.supplyAsync(() -> readOutput(connector));

            Assertions.assertEquals("ssh", output.get(10, TimeUnit.SECONDS).trim());
        } finally {
            connector.close();
        }
    }

    private static String readOutput(TtyConnector connector) {
        final StringBuilder output = new StringBuilder();
        final char[] buffer = new char[1024];
        try {
            int read;
            while ((read = connector.read(buffer, 0, buffer.length)) >= 0) {
                output.append(buffer, 0, read);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return output.toString();
    }
}
