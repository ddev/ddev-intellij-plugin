package de.php_perfect.intellij.ddev.cmd;

import com.intellij.execution.Executor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

final class DdevShellCommandHandlerTest extends BasePlatformTestCase {
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

    @ParameterizedTest
    @ValueSource(strings = {"cat abc", "ddev ", "ddev foo"})
    void invalidDdevCommands(String command) {
        final DdevShellCommandHandlerImpl ddevShellCommandHandlerImpl = new DdevShellCommandHandlerImpl();

        Assertions.assertFalse(ddevShellCommandHandlerImpl.matches(this.getProject(), null, true, command));
    }

    @Test
    void ddevCommand() {
        final DdevShellCommandHandlerImpl ddevShellCommandHandlerImpl = new DdevShellCommandHandlerImpl();

        Assertions.assertTrue(ddevShellCommandHandlerImpl.matches(this.getProject(), null, true, "ddev start"));
    }

    @ParameterizedTest
    @CsvSource({"start,START", "stop,STOP", "restart,RESTART", "poweroff,POWER_OFF", "delete,DELETE", "share,SHARE", "config,CONFIG"})
    void executesMatchingRunnerAction(String command, DdevShellCommandHandlerImpl.Action action) {
        final DdevRunner ddevRunner = Mockito.mock(DdevRunner.class);
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), DdevRunner.class, ddevRunner, getTestRootDisposable());

        Assertions.assertTrue(new DdevShellCommandHandlerImpl().execute(this.getProject(), null, true, "ddev " + command + " ", Mockito.mock(Executor.class)));

        final Project project = this.getProject();
        switch (action) {
            case START -> Mockito.verify(ddevRunner).start(project);
            case STOP -> Mockito.verify(ddevRunner).stop(project);
            case RESTART -> Mockito.verify(ddevRunner).restart(project);
            case POWER_OFF -> Mockito.verify(ddevRunner).powerOff(project);
            case DELETE -> Mockito.verify(ddevRunner).delete(project);
            case SHARE -> Mockito.verify(ddevRunner).share(project);
            case CONFIG -> Mockito.verify(ddevRunner).config(project);
        }
        Mockito.verifyNoMoreInteractions(ddevRunner);
    }

    @ParameterizedTest
    @ValueSource(strings = {"cat abc", "ddev foo"})
    void ignoresUnknownCommands(String command) {
        final DdevRunner ddevRunner = Mockito.mock(DdevRunner.class);
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), DdevRunner.class, ddevRunner, getTestRootDisposable());

        Assertions.assertFalse(new DdevShellCommandHandlerImpl().execute(this.getProject(), null, true, command, Mockito.mock(Executor.class)));
        Mockito.verifyNoInteractions(ddevRunner);
    }
}
