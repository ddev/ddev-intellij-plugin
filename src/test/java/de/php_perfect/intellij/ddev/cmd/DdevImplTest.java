package de.php_perfect.intellij.ddev.cmd;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.php_perfect.intellij.ddev.version.Version;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

final class DdevImplTest extends BasePlatformTestCase {
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
    void version() throws CommandFailedException {
        final Version expected = new Version("v1.22.0");
        final ProcessOutput processOutput = new ProcessOutput("ddev version v1.22.0", "", 0, false, false);

        final MockProcessExecutor mockProcessExecutor = (MockProcessExecutor) ApplicationManager.getApplication().getService(ProcessExecutor.class);
        mockProcessExecutor.addProcessOutput("ddev --version", processOutput);

        Assertions.assertEquals(expected, new DdevImpl().version("ddev", getProject()));
    }

    @Test
    void headVersion() throws CommandFailedException {
        final Version expected = new Version("v1.24.4-36-ge74e3a95f");
        final ProcessOutput processOutput = new ProcessOutput("ddev version v1.24.4-36-ge74e3a95f", "", 0, false, false);

        final MockProcessExecutor mockProcessExecutor = (MockProcessExecutor) ApplicationManager.getApplication().getService(ProcessExecutor.class);
        mockProcessExecutor.addProcessOutput("ddev --version", processOutput);

        Version actual = new DdevImpl().version("ddev", getProject());
        Assertions.assertEquals(expected, actual);
        Assertions.assertTrue(actual.isHeadVersion());
        Assertions.assertEquals("36-ge74e3a95f", actual.getBuildInfo());
    }

    @Test
    void detailedVersions() throws CommandFailedException, IOException {
        Versions expected = new Versions("v1.19.0", "20.10.12", "v2.2.2", "docker-desktop");

        ProcessOutput processOutput = new ProcessOutput(Files.readString(Path.of("src/test/resources/ddev_version.json")), "", 0, false, false);

        MockProcessExecutor mockProcessExecutor = (MockProcessExecutor) ApplicationManager.getApplication().getService(ProcessExecutor.class);
        mockProcessExecutor.addProcessOutput("ddev version --json-output", processOutput);

        Assertions.assertEquals(expected, new DdevImpl().detailedVersions("ddev", getProject()));
    }

    @Test
    void describe() throws CommandFailedException, IOException {
        Description expected = new Description("acol", "8.1", Description.Status.STOPPED, null, null, null, null, new HashMap<>(), null, "https://acol.ddev.site");

        ProcessOutput processOutput = new ProcessOutput(Files.readString(Path.of("src/test/resources/ddev_describe.json")), "", 0, false, false);

        MockProcessExecutor mockProcessExecutor = (MockProcessExecutor) ApplicationManager.getApplication().getService(ProcessExecutor.class);
        mockProcessExecutor.addProcessOutput("ddev describe --json-output", processOutput);

        Assertions.assertEquals(expected, new DdevImpl().describe("ddev", getProject()));
    }
}
