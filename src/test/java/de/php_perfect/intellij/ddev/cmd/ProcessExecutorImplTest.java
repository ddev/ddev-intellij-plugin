package de.php_perfect.intellij.ddev.cmd;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

final class ProcessExecutorImplTest extends BasePlatformTestCase {
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
    void capturesProcessOutput() throws ExecutionException {
        final GeneralCommandLine commandLine = new GeneralCommandLine(javaBinary(), "-version")
                .withWorkDirectory(System.getProperty("java.io.tmpdir"));

        final ProcessOutput output = new ProcessExecutorImpl().executeCommandLine(commandLine, 60_000, false);

        Assertions.assertEquals(0, output.getExitCode());
        Assertions.assertFalse(output.isTimeout());
        Assertions.assertTrue(output.getStderr().contains("version"), output.getStderr());
    }

    @Test
    void missingBinaryFails() {
        final String missingBinary = Path.of(System.getProperty("java.io.tmpdir"), "missing-ddev-" + System.nanoTime()).toString();
        final GeneralCommandLine commandLine = new GeneralCommandLine(missingBinary, "--version")
                .withWorkDirectory(System.getProperty("java.io.tmpdir"));

        Assertions.assertThrows(ExecutionException.class, () -> new ProcessExecutorImpl().executeCommandLine(commandLine, 60_000, false));
    }

    static String javaBinary() {
        return ProcessHandle.current().info().command().orElseThrow();
    }
}
