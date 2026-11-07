package de.php_perfect.intellij.ddev.php.composer;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.php_perfect.intellij.ddev.cmd.Description;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.jdom.Element;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
        final String workingDir = missingDirectory();

        final ExecutionException exception = Assertions.assertThrows(ExecutionException.class, () ->
                new DdevComposerExecution().createProcessHandler(this.getProject(), workingDir, List.of("install"), "composer install"));

        Assertions.assertTrue(exception.getMessage().startsWith("Failed to start DDEV Composer process: "));
    }

    @Test
    void projectBasePathIsDefaultWorkingDirectory() {
        final Project project = Mockito.mock(Project.class);
        Mockito.when(project.getBasePath()).thenReturn(missingDirectory());

        Assertions.assertThrows(ExecutionException.class, () ->
                new DdevComposerExecution().createProcessHandler(project, null, List.of("install"), "composer install"));

        Mockito.verify(project).getBasePath();
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

    private static String missingDirectory() {
        return Path.of(System.getProperty("java.io.tmpdir"), "missing-composer-dir-" + System.nanoTime()).toString();
    }
}
