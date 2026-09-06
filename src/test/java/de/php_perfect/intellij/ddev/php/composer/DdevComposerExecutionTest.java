package de.php_perfect.intellij.ddev.php.composer;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.php_perfect.intellij.ddev.cmd.wsl.WslAware;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

final class DdevComposerExecutionTest extends BasePlatformTestCase {
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
    void usesConfiguredBinaryAndStartsThePatchedProcess() throws Exception {
        final Project project = mock(Project.class);
        final DdevStateManager stateManager = mock(DdevStateManager.class);
        final State state = mock(State.class);
        when(project.getService(DdevStateManager.class)).thenReturn(stateManager);
        when(stateManager.getState()).thenReturn(state);
        when(state.getDdevBinary()).thenReturn("/custom/bin/ddev");
        when(project.getBasePath()).thenReturn("/ide-project");
        final var command = ArgumentCaptor.forClass(GeneralCommandLine.class);
        try (var wsl = mockStatic(WslAware.class)) {
            wsl.when(() -> WslAware.toHostPath("/composer-project", "/ide-project")).thenReturn("/host-project");
            wsl.when(() -> WslAware.patchCommandLine(any(GeneralCommandLine.class)))
                    .thenReturn(new GeneralCommandLine(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-version"));
            final var handler = new DdevComposerExecution().createProcessHandler(project,
                    "/composer-project", List.of("install", "--no-scripts"), "Composer install");
            try {
                handler.startNotify();
                assertThat(handler.waitFor(10_000)).isTrue();
                assertThat(handler.getExitCode()).isZero();
            } finally {
                if (!handler.isProcessTerminated()) handler.destroyProcess();
            }
            wsl.verify(() -> WslAware.patchCommandLine(command.capture()));
            assertThat(command.getValue().getExePath()).isEqualTo("/custom/bin/ddev");
            assertThat(command.getValue().getParametersList().getList()).containsExactly("composer", "install", "--no-scripts");
            assertThat(command.getValue().getWorkDirectory().getPath()).isEqualTo("/host-project");
        }
    }
}
