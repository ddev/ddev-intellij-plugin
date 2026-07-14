package de.php_perfect.intellij.ddev.wordpress;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    public static @Nullable Path findConfig(@NotNull Path projectRoot) {
        final Path ddevConfig = projectRoot.resolve("wp-config-ddev.php");

        if (Files.isRegularFile(ddevConfig)) {
            return ddevConfig;
        }

        final Path standardConfig = projectRoot.resolve("wp-config.php");
        return Files.isRegularFile(standardConfig) ? standardConfig : null;
    }

    public static @Nullable DebugState readDebugState(@NotNull Path projectRoot) throws IOException {
        final Path config = findConfig(projectRoot);

        if (config == null) {
            return null;
        }

        final String content = Files.readString(config);
        final boolean enabled = isTrue(content, "WP_DEBUG");
        final boolean silent = enabled && isTrue(content, "WP_DEBUG_LOG") && isFalse(content, "WP_DEBUG_DISPLAY");
        return new DebugState(enabled, silent);
    }

    public static @NotNull Path setDebugMode(@NotNull Path projectRoot, @NotNull DebugMode mode) throws IOException {
        final Path config = findConfig(projectRoot);

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
