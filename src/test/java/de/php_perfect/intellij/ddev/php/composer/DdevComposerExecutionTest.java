package de.php_perfect.intellij.ddev.php.composer;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.php_perfect.intellij.ddev.cmd.Description;
import de.php_perfect.intellij.ddev.cmd.wsl.WslAware;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.jdom.Element;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.List;

final class DdevComposerExecutionTest extends BasePlatformTestCase {
    private State state;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        this.state = Mockito.mock(State.class);
        final DdevStateManager stateManager = Mockito.mock(DdevStateManager.class);
        Mockito.when(stateManager.getState()).thenReturn(this.state);
        ServiceContainerUtil.replaceService(this.getProject(), DdevStateManager.class, stateManager, this.getTestRootDisposable());
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void processCreationFailureIsReported() {
        Mockito.when(this.state.getDdevBinary()).thenReturn("ddev");
        final String workingDir = missingDirectory();

        final ExecutionException exception = Assertions.assertThrows(ExecutionException.class, () ->
                new DdevComposerExecution().createProcessHandler(this.getProject(), workingDir, List.of("install"), "composer install"));

        Assertions.assertTrue(exception.getMessage().startsWith("Failed to start DDEV Composer process: "));
    }

    @Test
    void projectBasePathIsDefaultWorkingDirectory() {
        final Project project = mockProjectWithBinary("ddev");
        Mockito.when(project.getBasePath()).thenReturn(missingDirectory());

        Assertions.assertThrows(ExecutionException.class, () ->
                new DdevComposerExecution().createProcessHandler(project, null, List.of("install"), "composer install"));

        Mockito.verify(project).getBasePath();
    }

    @Test
    void usesConfiguredBinaryAndStartsThePatchedProcess() throws Exception {
        final Project project = mockProjectWithBinary("/custom/bin/ddev");
        Mockito.when(project.getBasePath()).thenReturn("/ide-project");
        final ArgumentCaptor<GeneralCommandLine> command = ArgumentCaptor.forClass(GeneralCommandLine.class);

        try (MockedStatic<WslAware> wsl = Mockito.mockStatic(WslAware.class)) {
            wsl.when(() -> WslAware.toHostPath("/composer-project", "/ide-project")).thenReturn("/host-project");
            wsl.when(() -> WslAware.patchCommandLine(Mockito.any(GeneralCommandLine.class)))
                    .thenReturn(new GeneralCommandLine(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-version"));
            final ProcessHandler handler = new DdevComposerExecution().createProcessHandler(project,
                    "/composer-project", List.of("install", "--no-scripts"), "Composer install");
            try {
                handler.startNotify();
                Assertions.assertTrue(handler.waitFor(10_000));
                Assertions.assertEquals(0, handler.getExitCode());
            } finally {
                if (!handler.isProcessTerminated()) {
                    handler.destroyProcess();
                }
            }

            wsl.verify(() -> WslAware.patchCommandLine(command.capture()));
            Assertions.assertEquals("/custom/bin/ddev", command.getValue().getExePath());
            Assertions.assertEquals(List.of("composer", "install", "--no-scripts"), command.getValue().getParametersList().getList());
            Assertions.assertEquals("/host-project", command.getValue().getWorkDirectory().getPath());
        }
    }

    @Test
    void cancelDestroysProcess() {
        final ProcessHandler handler = Mockito.mock(ProcessHandler.class);

        new DdevComposerExecution().cancelProcess(handler);

        Mockito.verify(handler).destroyProcess();
    }

    @Test
    void runningProjectIsWellConfigured() {
        this.mockState(true, true, Description.builder().status(Description.Status.RUNNING).build());

        Assertions.assertNull(new DdevComposerExecution().getValidationMessage(this.getProject()));
        Assertions.assertTrue(new DdevComposerExecution().isWellConfigured(this.getProject(), true));
    }

    @Test
    void missingBinaryIsReported() {
        this.mockState(false, true, null);

        Assertions.assertEquals("DDEV binary is not configured", new DdevComposerExecution().getValidationMessage(this.getProject()));
        Assertions.assertFalse(new DdevComposerExecution().isWellConfigured(this.getProject(), true));
    }

    @Test
    void unconfiguredProjectIsReported() {
        this.mockState(true, false, null);

        Assertions.assertEquals("DDEV project is not configured", new DdevComposerExecution().getValidationMessage(this.getProject()));
    }

    @Test
    void missingDescriptionIsReportedAsNotRunning() {
        this.mockState(true, true, null);

        Assertions.assertEquals("DDEV is not running. Please start DDEV before using Composer commands.", new DdevComposerExecution().getValidationMessage(this.getProject()));
    }

    @Test
    void stoppedProjectIsReportedAsNotRunning() {
        this.mockState(true, true, Description.builder().status(Description.Status.STOPPED).build());

        Assertions.assertEquals("DDEV is not running. Please start DDEV before using Composer commands.", new DdevComposerExecution().getValidationMessage(this.getProject()));
    }

    @Test
    void stateFailureIsReported() {
        Mockito.when(this.state.isBinaryConfigured()).thenThrow(new IllegalStateException("broken"));

        Assertions.assertEquals("Failed to validate DDEV configuration: broken", new DdevComposerExecution().getValidationMessage(this.getProject()));
    }

    @Test
    void hasNoInterpreter() {
        Assertions.assertNull(new DdevComposerExecution().getInterpreterId());
    }

    @Test
    void savedExecutionLoadsAgain() {
        final Element element = new DdevComposerExecution().save();

        Assertions.assertEquals("ddev", element.getName());
        Assertions.assertEquals("ddev", element.getAttributeValue("type"));
        Assertions.assertEquals(new DdevComposerExecution(), DdevComposerExecution.load(element));
    }

    @Test
    void elementWithoutTypeLoads() {
        Assertions.assertNotNull(DdevComposerExecution.load(new Element("ddev")));
    }

    @Test
    void foreignElementsAreRejected() {
        Assertions.assertNull(DdevComposerExecution.load(new Element("local")));
        Assertions.assertNull(DdevComposerExecution.load(new Element("ddev").setAttribute("type", "remote")));
    }

    @Test
    void executionsAreEqualByType() {
        final DdevComposerExecution execution = new DdevComposerExecution();

        Assertions.assertEquals(execution, execution);
        Assertions.assertEquals(new DdevComposerExecution(), execution);
        Assertions.assertEquals(new DdevComposerExecution().hashCode(), execution.hashCode());
        Assertions.assertNotEquals(null, execution);
        Assertions.assertNotEquals("ddev", execution);
        Assertions.assertEquals("DdevComposerExecution{}", execution.toString());
    }

    private void mockState(boolean binaryConfigured, boolean configured, Description description) {
        Mockito.when(this.state.isBinaryConfigured()).thenReturn(binaryConfigured);
        Mockito.when(this.state.isConfigured()).thenReturn(configured);
        Mockito.when(this.state.getDescription()).thenReturn(description);
    }

    private static Project mockProjectWithBinary(String binary) {
        final Project project = Mockito.mock(Project.class);
        final DdevStateManager stateManager = Mockito.mock(DdevStateManager.class);
        final State state = Mockito.mock(State.class);
        Mockito.when(project.getService(DdevStateManager.class)).thenReturn(stateManager);
        Mockito.when(stateManager.getState()).thenReturn(state);
        Mockito.when(state.getDdevBinary()).thenReturn(binary);

        return project;
    }

    private static String missingDirectory() {
        return Path.of(System.getProperty("java.io.tmpdir"), "missing-composer-dir-" + System.nanoTime()).toString();
    }
}
