package de.php_perfect.intellij.ddev.util;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

final class PluginDisplayNameMapperTest extends BasePlatformTestCase {
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
    @CsvSource({
            "com.jetbrains.php,PHP",
            "org.jetbrains.plugins.phpstorm-remote-interpreter,PHP Remote Interpreter",
            "org.jetbrains.plugins.phpstorm-docker,PHP Docker",
            "Docker,Docker",
            "NodeJS,Node.js",
            "org.jetbrains.plugins.node-remote-interpreter,Node.js Remote Interpreter",
            "com.intellij.database,Database Tools and SQL",
            "org.jetbrains.plugins.terminal,Terminal",
    })
    void knownPluginsUseBundleName(String pluginId, String displayName) {
        Assertions.assertEquals(displayName, PluginDisplayNameMapper.getDisplayName(pluginId));
    }

    @Test
    void unknownPluginKeepsId() {
        Assertions.assertEquals("com.example.unknown", PluginDisplayNameMapper.getDisplayName("com.example.unknown"));
    }

    @Test
    void everyRequiredPluginHasDisplayName() {
        FeatureRequiredPlugins.PHP_INTERPRETER.forEach(id -> Assertions.assertNotEquals(id, PluginDisplayNameMapper.getDisplayName(id)));
        FeatureRequiredPlugins.NODE_INTERPRETER.forEach(id -> Assertions.assertNotEquals(id, PluginDisplayNameMapper.getDisplayName(id)));
        FeatureRequiredPlugins.DATABASE.forEach(id -> Assertions.assertNotEquals(id, PluginDisplayNameMapper.getDisplayName(id)));
    }
}
