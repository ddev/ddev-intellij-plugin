package de.php_perfect.intellij.ddev.php.composer;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

final class DdevComposerProcessHandlerTest extends BasePlatformTestCase {
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
    void reportsProcessExit() throws Exception {
        final String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        final Process process = new ProcessBuilder(java, "-version").start();

        final DdevComposerProcessHandler handler = new DdevComposerProcessHandler(process, "java -version");
        handler.startNotify();

        Assertions.assertTrue(handler.waitFor(30_000));
        Assertions.assertEquals(0, handler.getExitCode());
        Assertions.assertEquals("java -version", handler.getCommandLine());
    }
}
