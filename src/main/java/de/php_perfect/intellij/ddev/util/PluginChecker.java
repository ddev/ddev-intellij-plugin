package de.php_perfect.intellij.ddev.util;

import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.notification.DdevNotifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to check for required plugins.
 */
public final class PluginChecker {
    private PluginChecker() {
        // Utility class, no instances
    }

    /**
     * Checks if any required plugin is missing.
     * If any plugins are missing, it will notify the user and return true.
     *
     * @param project The current project
     * @param requiredPlugins List of plugin IDs to check
     * @param featureName The name of the feature that requires these plugins
     * @return true if any plugin is missing, false if all are available
     */
    public static boolean isMissingRequiredPlugins(@NotNull Project project, @NotNull List<String> requiredPlugins, @NotNull String featureName) {
        List<String> missingPluginNames = getMissingPlugins(requiredPlugins);

        if (!missingPluginNames.isEmpty()) {
            String missingPluginsDisplay = String.join(", ", missingPluginNames);
            if (missingPluginNames.size() == 1) {
                DdevNotifier.getInstance(project).notifyMissingPlugin(missingPluginsDisplay, featureName);
            } else {
                DdevNotifier.getInstance(project).notifyMissingPlugins(missingPluginsDisplay, featureName);
            }
            return true;
        }

        return false;
    }

    /**
     * Checks if any required plugin is missing without showing notifications.
     * This is useful for UI components that want to check dependencies without showing notifications.
     *
     * @param requiredPlugins List of plugin IDs to check
     * @return List of missing plugin display names, empty if all are available
     */
    public static @NotNull List<String> getMissingPlugins(@NotNull List<String> requiredPlugins) {
        final var pluginManager = PluginManager.getInstance();
        final List<String> missingPluginNames = new ArrayList<>();

        for (final String id : requiredPlugins) {
            final PluginId pluginId = PluginId.findId(id);

            if (pluginId == null || pluginManager.findEnabledPlugin(pluginId) == null) {
                String displayName = PluginDisplayNameMapper.getDisplayName(id);
                missingPluginNames.add(displayName);
            }
        }

        return missingPluginNames;
    }
}
