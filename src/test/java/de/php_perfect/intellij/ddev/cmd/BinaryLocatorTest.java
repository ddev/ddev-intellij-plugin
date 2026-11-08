package de.php_perfect.intellij.ddev.cmd;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class BinaryLocatorTest extends BasePlatformTestCase {
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
    void findBinary() {
        String expectedWhich = "which";
        if (SystemInfo.isWindows) {
            expectedWhich = "where";
        }

        ProcessOutput processOutput = new ProcessOutput("/foo/bar/bin/ddev", "", 0, false, false);

        MockProcessExecutor mockProcessExecutor = (MockProcessExecutor) ApplicationManager.getApplication().getService(ProcessExecutor.class);
        mockProcessExecutor.addProcessOutput(expectedWhich + " ddev", processOutput);

        Assertions.assertEquals("/foo/bar/bin/ddev", new BinaryLocatorImpl().findInPath(getProject()));
    }

    @Test
    void isNotInstalled() {
        String expectedWhich = "which";
        if (SystemInfo.isWindows) {
            expectedWhich = "where";
        }

        ProcessOutput processOutput = new ProcessOutput("", "", 1, false, false);

        MockProcessExecutor mockProcessExecutor = (MockProcessExecutor) ApplicationManager.getApplication().getService(ProcessExecutor.class);
        mockProcessExecutor.addProcessOutput(expectedWhich + " ddev", processOutput);

        Assertions.assertNull(new BinaryLocatorImpl().findInPath(getProject()));
    }

    @Test
    void lookupFailure() {
        final String which = SystemInfo.isWindows ? "where" : "which";

        MockProcessExecutor mockProcessExecutor = (MockProcessExecutor) ApplicationManager.getApplication().getService(ProcessExecutor.class);
        mockProcessExecutor.addProcessFailure(which + " ddev", new ExecutionException("not startable"));

        Assertions.assertNull(new BinaryLocatorImpl().findInPath(getProject()));
    }
}
