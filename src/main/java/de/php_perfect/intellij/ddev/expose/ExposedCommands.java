package de.php_perfect.intellij.ddev.expose;

import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.settings.DdevApplicationSettings;
import de.php_perfect.intellij.ddev.settings.DdevSettingsState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The container commands exposed in the IDE terminal. An entry is a command name, looked up on the
 * web container's PATH (which includes the project's {@code vendor/bin}), or a container path;
 * relative paths are resolved against the project root in the container.
 */
public final class ExposedCommands {
    public static final @NotNull String CONTAINER_ROOT = "/var/www/html";

    /**
     * Commands offered for exposing without typing them in.
     */
    public static final @NotNull List<String> SUGGESTED = List.of(
            "artisan", "blackfire", "cake", "composer", "console", "craft", "drush", "magento", "mysqldump",
            "node", "npm", "nvm", "php", "pint", "pnpm", "python", "rsync", "sake", "typo3", "wp", "xdebug",
            "xhprof", "yarn");

    private static final @NotNull Pattern COMMAND_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._+-]*");
    private static final @NotNull Pattern CONTAINER_PATH = Pattern.compile("[A-Za-z0-9._+/-]+");

    public record Launcher(@NotNull String name, @NotNull String command) {
    }

    private ExposedCommands() {
    }

    /**
     * Returns the launcher for an entry, or {@code null} when the entry is neither a plain command
     * name nor a container path.
     */
    public static @Nullable Launcher parse(@NotNull String entry) {
        final String trimmed = entry.trim();

        if (COMMAND_NAME.matcher(trimmed).matches()) {
            return new Launcher(trimmed, trimmed);
        }
        if (!trimmed.contains("/") || !CONTAINER_PATH.matcher(trimmed).matches() || trimmed.contains("..")) {
            return null;
        }

        final String name = trimmed.substring(trimmed.lastIndexOf('/') + 1);
        if (!COMMAND_NAME.matcher(name).matches()) {
            return null;
        }
        final String path = trimmed.startsWith("/") ? trimmed
                : CONTAINER_ROOT + "/" + (trimmed.startsWith("./") ? trimmed.substring(2) : trimmed);
        return new Launcher(name, path);
    }

    /**
     * Combines global and project entries into launchers; a project entry replaces a global one of the same name.
     */
    public static @NotNull List<Launcher> launchers(@NotNull Collection<String> global, @NotNull Collection<String> project) {
        final Map<String, Launcher> launchers = new LinkedHashMap<>();
        for (Collection<String> entries : List.of(global, project)) {
            for (String entry : entries) {
                final Launcher launcher = parse(entry);
                if (launcher != null) {
                    launchers.put(launcher.name(), launcher);
                }
            }
        }
        return new ArrayList<>(launchers.values());
    }

    public static @NotNull List<Launcher> launchers(@NotNull Project project) {
        return launchers(DdevApplicationSettings.getInstance().exposedCommands,
                DdevSettingsState.getInstance(project).exposedCommands);
    }
}
