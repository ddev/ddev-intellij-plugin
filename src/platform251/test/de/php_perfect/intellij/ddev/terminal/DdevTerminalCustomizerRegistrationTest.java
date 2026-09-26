package de.php_perfect.intellij.ddev.terminal;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class DdevTerminalCustomizerRegistrationTest extends BasePlatformTestCase {
    private static final ExtensionPointName<Object> EP_NAME = ExtensionPointName.create("org.jetbrains.plugins.terminal.localTerminalCustomizer");

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
    void customizerIsRegisteredForThePlatformExtensionPoint() {
        assertThat(EP_NAME.getExtensionList()).hasAtLeastOneElementOfType(DdevTerminalCustomizer.class);
    }
}
