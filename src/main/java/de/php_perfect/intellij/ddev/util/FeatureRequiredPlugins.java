package de.php_perfect.intellij.ddev.util;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Utility class to hold the required plugins for each feature.
 */
public final class FeatureRequiredPlugins {
    private FeatureRequiredPlugins() {
        // Utility class, no instances
    }

    /**
     * Required plugins for PHP interpreter auto-registration.
     */
    public static final @NotNull List<String> PHP_INTERPRETER = List.of(
            "com.jetbrains.php",
            "org.jetbrains.plugins.phpstorm-remote-interpreter",
            "org.jetbrains.plugins.phpstorm-docker"
    );

    /**
     * Required plugins for Node.js interpreter auto-registration.
     */
    public static final @NotNull List<String> NODE_INTERPRETER = List.of(
            "NodeJS",
            "org.jetbrains.plugins.node-remote-interpreter"
    );

    /**
     * Required plugins for database auto-registration.
     */
    public static final @NotNull List<String> DATABASE = List.of(
            "com.intellij.database"
    );
}
