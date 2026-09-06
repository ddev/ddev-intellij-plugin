package de.php_perfect.intellij.ddev.wordpress;

import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.cmd.Description;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WordPressConfigManager {
    public enum DebugMode {
        ENABLED,
        SILENT,
        DISABLED
    }

    public record DebugState(boolean enabled, boolean silent) {
    }

    private WordPressConfigManager() {
    }

    public static @Nullable String docroot(@NotNull Project project) {
        final Description description = DdevStateManager.getInstance(project).getState().getDescription();
        return description == null ? null : description.getDocroot();
    }

    public static @NotNull Path documentRoot(@NotNull Path projectRoot, @Nullable String docroot) {
        return docroot == null || docroot.isBlank() ? projectRoot : projectRoot.resolve(docroot).normalize();
    }

    public static @Nullable Path findFile(@NotNull Path projectRoot, @Nullable String docroot,
                                          @NotNull String relativePath) {
        final Path documentRoot = documentRoot(projectRoot, docroot);
        final Path file = documentRoot.resolve(relativePath);
        if (Files.isRegularFile(file)) {
            return file;
        }
        // WordPress also supports wp-config.php in the parent of its document root.
        if ((relativePath.equals("wp-config.php") || relativePath.equals("wp-config-ddev.php"))
                && documentRoot.getParent() != null
                && documentRoot.getParent().startsWith(projectRoot.normalize())) {
            final Path parentConfig = documentRoot.getParent().resolve(relativePath);
            if (Files.isRegularFile(parentConfig)) {
                return parentConfig;
            }
        }
        return null;
    }

    public static @Nullable Path findConfig(@NotNull Path projectRoot) {
        return findConfig(projectRoot, null);
    }

    public static @Nullable Path findConfig(@NotNull Path projectRoot, @Nullable String docroot) {
        final Path documentRoot = documentRoot(projectRoot, docroot);
        for (String name : List.of("wp-config-ddev.php", "wp-config.php")) {
            final Path file = documentRoot.resolve(name);
            if (Files.isRegularFile(file)) return file;
        }
        final Path ddevConfig = findFile(projectRoot, docroot, "wp-config-ddev.php");

        if (ddevConfig != null) {
            return ddevConfig;
        }

        return findFile(projectRoot, docroot, "wp-config.php");
    }

    public static @Nullable DebugState readDebugState(@NotNull Path projectRoot) throws IOException {
        return readDebugState(projectRoot, null);
    }

    public static @Nullable DebugState readDebugState(@NotNull Path projectRoot, @Nullable String docroot) throws IOException {
        final Path config = findConfig(projectRoot, docroot);

        if (config == null) {
            return null;
        }

        final String content = Files.readString(config);
        final boolean enabled = isTrue(content, "WP_DEBUG");
        final boolean silent = enabled && isTrue(content, "WP_DEBUG_LOG") && isFalse(content, "WP_DEBUG_DISPLAY");
        return new DebugState(enabled, silent);
    }

    public static @NotNull Path setDebugMode(@NotNull Path projectRoot, @NotNull DebugMode mode) throws IOException {
        return setDebugMode(projectRoot, null, mode);
    }

    public static @NotNull Path setDebugMode(@NotNull Path projectRoot, @Nullable String docroot,
                                           @NotNull DebugMode mode) throws IOException {
        final Path config = findConfig(projectRoot, docroot);

        if (config == null) {
            throw new IOException("No wp-config.php or wp-config-ddev.php found in " + projectRoot);
        }

        String content = Files.readString(config);
        content = setBoolean(content, "WP_DEBUG", mode != DebugMode.DISABLED);

        if (mode == DebugMode.SILENT) {
            content = setBoolean(content, "WP_DEBUG_LOG", true);
            content = setBoolean(content, "WP_DEBUG_DISPLAY", false);
        } else {
            content = removeConstant(content, "WP_DEBUG_LOG");
            content = removeConstant(content, "WP_DEBUG_DISPLAY");
        }

        Files.writeString(config, content);
        return config;
    }

    private static boolean isTrue(@NotNull String content, @NotNull String name) {
        final String value = constantValue(content, name);
        return value != null && "true".equalsIgnoreCase(value.trim());
    }

    private static boolean isFalse(@NotNull String content, @NotNull String name) {
        final String value = constantValue(content, name);
        return value != null && "false".equalsIgnoreCase(value.trim());
    }

    private static @Nullable String constantValue(@NotNull String content, @NotNull String name) {
        final Matcher matcher = constantPattern(name, true).matcher(content);
        return matcher.find() ? matcher.group(2) : null;
    }

    private static @NotNull String setBoolean(@NotNull String content, @NotNull String name, boolean value) {
        final Matcher matcher = constantPattern(name, false).matcher(content);
        final String definition = "define( '" + name + "', " + value + " );";

        if (matcher.find()) {
            return matcher.replaceFirst(Matcher.quoteReplacement(definition));
        }

        return insertDefinition(content, definition);
    }

    private static @NotNull String removeConstant(@NotNull String content, @NotNull String name) {
        return constantPattern(name, false).matcher(content).replaceAll("")
                .replaceFirst("(?:\\R[\\t ]*){3,}", System.lineSeparator() + System.lineSeparator());
    }

    private static @NotNull Pattern constantPattern(@NotNull String name, boolean captureValue) {
        final String value = captureValue ? "([^;\\r\\n]+?)" : "[^;\\r\\n]+?";
        return Pattern.compile("(?m)^[\\t ]*define\\s*\\(\\s*(['\"])" + Pattern.quote(name)
                + "\\1\\s*,\\s*" + value + "\\s*\\)\\s*;[\\t ]*(?=\\R|$)");
    }

    private static @NotNull String insertDefinition(@NotNull String content, @NotNull String definition) {
        final String newline = content.contains("\r\n") ? "\r\n" : "\n";
        final Pattern anchor = Pattern.compile("(?m)^[\\t ]*/\\*(?:\\*|\\s+That's all)");
        final Matcher matcher = anchor.matcher(content);

        if (matcher.find()) {
            return content.substring(0, matcher.start()) + definition + newline + newline + content.substring(matcher.start());
        }

        final int phpClose = content.lastIndexOf("?>");
        final int insertionPoint = phpClose >= 0 ? phpClose : content.length();
        final String separator = insertionPoint > 0 && !content.substring(0, insertionPoint).endsWith(newline) ? newline : "";
        return content.substring(0, insertionPoint) + separator + definition + newline + content.substring(insertionPoint);
    }
}
