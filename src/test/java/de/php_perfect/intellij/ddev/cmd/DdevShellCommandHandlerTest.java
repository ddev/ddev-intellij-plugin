package de.php_perfect.intellij.ddev.cmd;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
}
