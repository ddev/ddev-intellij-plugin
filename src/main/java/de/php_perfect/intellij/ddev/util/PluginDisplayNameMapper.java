package de.php_perfect.intellij.ddev.util;

import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to map plugin IDs to display names.
 */
public final class PluginDisplayNameMapper {
    private static final Map<String, String> PLUGIN_DISPLAY_NAMES = new HashMap<>();

    static {
        // Initialize with known plugin IDs and their message bundle keys
        PLUGIN_DISPLAY_NAMES.put("com.jetbrains.php", "plugin.name.php");
        PLUGIN_DISPLAY_NAMES.put("org.jetbrains.plugins.phpstorm-remote-interpreter", "plugin.name.phpstorm-remote-interpreter");
        PLUGIN_DISPLAY_NAMES.put("org.jetbrains.plugins.phpstorm-docker", "plugin.name.phpstorm-docker");
        PLUGIN_DISPLAY_NAMES.put("Docker", "plugin.name.docker");
        PLUGIN_DISPLAY_NAMES.put("NodeJS", "plugin.name.nodejs");
        PLUGIN_DISPLAY_NAMES.put("org.jetbrains.plugins.node-remote-interpreter", "plugin.name.node-remote-interpreter");
        PLUGIN_DISPLAY_NAMES.put("com.intellij.database", "plugin.name.database");
        PLUGIN_DISPLAY_NAMES.put("org.jetbrains.plugins.terminal", "plugin.name.terminal");
    }

    private PluginDisplayNameMapper() {
        // Utility class, no instances
    }

    /**
     * Get the display name for a plugin ID.
     * First tries to get a localized name from the message bundle.
     * If not found, returns the plugin ID.
     *
     * @param pluginId The plugin ID
     * @return The display name
     */
    public static @NotNull String getDisplayName(@NotNull String pluginId) {
        // First try to get a localized name from our message bundle
        if (PLUGIN_DISPLAY_NAMES.containsKey(pluginId)) {
            String messageKey = PLUGIN_DISPLAY_NAMES.get(pluginId);
            String localizedName = DdevIntegrationBundle.message(messageKey);

            if (!localizedName.equals(messageKey)) {
                return localizedName;
            }
        }

        // Fall back to plugin ID if we couldn't find a label
        return pluginId;
    }
}
