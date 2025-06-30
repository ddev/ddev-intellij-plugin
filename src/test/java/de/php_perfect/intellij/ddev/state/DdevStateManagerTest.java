package de.php_perfect.intellij.ddev.state;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.php_perfect.intellij.ddev.cmd.Description;
import de.php_perfect.intellij.ddev.cmd.MockProcessExecutor;
import de.php_perfect.intellij.ddev.cmd.ProcessExecutor;
import de.php_perfect.intellij.ddev.version.Version;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

final class DdevStateManagerTest extends BasePlatformTestCase {
    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Test
    void testInitialize() {
        String expectedWhich = "which";
        if (SystemInfo.isWindows) {
            expectedWhich = "where";
        }

        final Project project = this.getProject();
        final MockDdevConfigLoader ddevConfigLoader = (MockDdevConfigLoader) DdevConfigLoader.getInstance(project);
        final MockProcessExecutor mockProcessExecutor = (MockProcessExecutor) ApplicationManager.getApplication().getService(ProcessExecutor.class);
        mockProcessExecutor.addProcessOutput("docker info", new ProcessOutput(0));
        mockProcessExecutor.addProcessOutput(expectedWhich + " ddev", new ProcessOutput("/foo/bar/bin/ddev", "", 0, false, false));

        ddevConfigLoader.setExists(true);
        this.prepareCommand("/foo/bar/bin/ddev --version", "ddev version v1.19.0");
        this.prepareCommandWithOutputFromFile("/foo/bar/bin/ddev describe --json-output", "src/test/resources/ddev_describe.json");

        DdevStateManager ddevStateManager = DdevStateManager.getInstance(project);
        ddevStateManager.initialize();

        StateImpl expectedState = new StateImpl();
        expectedState.setDdevBinary("/foo/bar/bin/ddev");
        expectedState.setConfigured(true);
        expectedState.setDdevVersion(new Version("v1.19.0"));
        expectedState.setDescription(Description.builder()
                .name("acol")
                .phpVersion("8.1")
                .status(Description.Status.STOPPED)
                .services(new HashMap<>())
                .primaryUrl("https://acol.ddev.site")
                .build());

        Assertions.assertEquals(expectedState, ddevStateManager.getState());
    }

    @Test
    void testReinitialize() {
        final Project project = this.getProject();

        final MockDdevConfigLoader ddevConfigLoader = (MockDdevConfigLoader) DdevConfigLoader.getInstance(project);
        ddevConfigLoader.setExists(true);

        final DdevStateManager ddevStateManager = DdevStateManager.getInstance(project);
        ddevStateManager.reinitialize();

        StateImpl expectedState = new StateImpl();
        expectedState.setDdevBinary("");
        expectedState.setConfigured(true);
        expectedState.setDdevVersion(null);
        expectedState.setDescription(null);

        Assertions.assertEquals(expectedState, ddevStateManager.getState());
    }

    @Test
    void testUpdateDescription() {
        String expectedWhich = "which";
        if (SystemInfo.isWindows) {
            expectedWhich = "where";
        }

        final Project project = this.getProject();
        final MockDdevConfigLoader ddevConfigLoader = (MockDdevConfigLoader) DdevConfigLoader.getInstance(project);
        MockProcessExecutor mockProcessExecutor = (MockProcessExecutor) ApplicationManager.getApplication().getService(ProcessExecutor.class);
        mockProcessExecutor.addProcessOutput("docker info", new ProcessOutput(0));
        mockProcessExecutor.addProcessOutput(expectedWhich + " ddev", new ProcessOutput("/foo/bar/bin/ddev", "", 0, false, false));

        ddevConfigLoader.setExists(true);
        this.prepareCommand("/foo/bar/bin/ddev --version", "ddev version v1.19.0");
        this.prepareCommandWithOutputFromFile("/foo/bar/bin/ddev describe --json-output", "src/test/resources/ddev_describe.json");

        DdevStateManager ddevStateManager = DdevStateManager.getInstance(this.getProject());
        ddevStateManager.initialize();

        StateImpl expectedState = new StateImpl();
        expectedState.setDdevBinary("/foo/bar/bin/ddev");
        expectedState.setConfigured(true);
        expectedState.setDdevVersion(new Version("v1.19.0"));
        expectedState.setDescription(Description.builder()
                .name("acol")
                .phpVersion("8.1")
                .status(Description.Status.STOPPED)
                .services(new HashMap<>())
                .primaryUrl("https://acol.ddev.site")
                .build());

        Assertions.assertEquals(expectedState, ddevStateManager.getState());

        this.prepareCommandWithOutputFromFile("/foo/bar/bin/ddev describe --json-output", "src/test/resources/ddev_describe2.json");

        ddevStateManager.updateDescription();

        expectedState.setDescription(Description.builder()
                .name("acol")
                .phpVersion("7.4")
                .status(Description.Status.STOPPED)
                .services(new HashMap<>())
                .primaryUrl("https://acol.ddev.site")
                .build());

        Assertions.assertEquals(expectedState, ddevStateManager.getState());
    }

    private void prepareCommand(String command, String output) {
        final MockProcessExecutor mockProcessExecutor = (MockProcessExecutor) ApplicationManager.getApplication().getService(ProcessExecutor.class);

        final ProcessOutput processOutput = new ProcessOutput(output, "", 0, false, false);
        mockProcessExecutor.addProcessOutput(command, processOutput);
    }

    private void prepareCommandWithOutputFromFile(String command, String file) {
        try {
            this.prepareCommand(command, Files.readString(Path.of(file)));
        } catch (IOException e) {
            Assertions.fail(e);
        }
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
