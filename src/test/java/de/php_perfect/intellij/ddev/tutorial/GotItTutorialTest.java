package de.php_perfect.intellij.ddev.tutorial;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;

final class GotItTutorialTest extends BasePlatformTestCase {
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
    void applicationServiceIsGotItTutorialImpl() {
        Assertions.assertInstanceOf(GotItTutorialImpl.class, GotItTutorial.getInstance());
    }

    @Test
    void statusBarTutorialAttachesToComponent() {
        final GotItTutorial tutorial = new GotItTutorialImpl();
        final JLabel component = new JLabel();

        Assertions.assertDoesNotThrow(() -> tutorial.showStatusBarTutorial(component, getTestRootDisposable()));
    }

    @Test
    void terminalTutorialAttachesToComponent() {
        final GotItTutorial tutorial = new GotItTutorialImpl();
        final JButton component = new JButton();

        Assertions.assertDoesNotThrow(() -> tutorial.showTerminalTutorial(component, getTestRootDisposable()));
    }
}
