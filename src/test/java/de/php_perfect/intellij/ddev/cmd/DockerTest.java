package de.php_perfect.intellij.ddev.cmd;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class DockerTest extends BasePlatformTestCase {
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
    void dockerIsRunning() {
        ProcessOutput processOutput = new ProcessOutput(0);

        MockProcessExecutor mockProcessExecutor = (MockProcessExecutor) ApplicationManager.getApplication().getService(ProcessExecutor.class);
        mockProcessExecutor.addProcessOutput("docker info", processOutput);

        Assertions.assertTrue(new DockerImpl().isRunning(this.getProject().getBasePath()));
    }

    @Test
    void dockerIsNotRunning() {
        ProcessOutput processOutput = new ProcessOutput(1);

        MockProcessExecutor mockProcessExecutor = (MockProcessExecutor) ApplicationManager.getApplication().getService(ProcessExecutor.class);
        mockProcessExecutor.addProcessOutput("docker info", processOutput);

        Assertions.assertFalse(new DockerImpl().isRunning(this.getProject().getBasePath()));
    }

    @Test
    void dockerCannotBeStarted() {
        MockProcessExecutor mockProcessExecutor = (MockProcessExecutor) ApplicationManager.getApplication().getService(ProcessExecutor.class);
        mockProcessExecutor.addProcessFailure("docker info", new ExecutionException("not startable"));

        Assertions.assertFalse(new DockerImpl().isRunning(this.getProject().getBasePath()));
    }

    @Test
    void context() {
        MockProcessExecutor mockProcessExecutor = (MockProcessExecutor) ApplicationManager.getApplication().getService(ProcessExecutor.class);
        mockProcessExecutor.addProcessOutput("docker context show", new ProcessOutput("desktop-linux\n", "", 0, false, false));

        Assertions.assertEquals("desktop-linux\n", new DockerImpl().getContext(this.getProject().getBasePath()));
    }

    @Test
    void contextOfUnstartableDocker() {
        MockProcessExecutor mockProcessExecutor = (MockProcessExecutor) ApplicationManager.getApplication().getService(ProcessExecutor.class);
        mockProcessExecutor.addProcessFailure("docker context show", new ExecutionException("not startable"));

        Assertions.assertEquals("", new DockerImpl().getContext(this.getProject().getBasePath()));
    }
}
