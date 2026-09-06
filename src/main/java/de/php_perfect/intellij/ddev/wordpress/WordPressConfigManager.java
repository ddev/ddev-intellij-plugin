package de.php_perfect.intellij.ddev.wordpress;

import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.cmd.Description;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WordPressConfigManager {
    /**
     * DDEV may overwrite any file that still carries this signature, so wp-config.php loses it once
     * the plugin writes settings into it. wp-config-ddev.php keeps it: DDEV has to keep regenerating
     * that file, and wp-config.php overrides its guarded definitions.
     */
    private static final String DDEV_GENERATED_SIGNATURE = "#ddev-generated";
    private static final Pattern DDEV_GENERATED_HEADER = Pattern.compile("(?m)^[\\t #*/]*" + DDEV_GENERATED_SIGNATURE + "[^\\r\\n]*\\R?"
            + "(?:^[\\t *]*ddev manages this file[^\\r\\n]*\\R?)?"
            + "(?:^[\\t *]*It is recommended that you leave this file alone\\.[\\t ]*\\R?)?");

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

    /**
     * Reads the effective debug state. wp-config.php is evaluated before it includes
     * wp-config-ddev.php, whose definitions are guarded with {@code defined( 'X' ) ||}, so a
     * definition in wp-config.php takes precedence.
     */
    public static @Nullable DebugState readDebugState(@NotNull Path projectRoot, @Nullable String docroot) throws IOException {
        final Path config = findFile(projectRoot, docroot, "wp-config.php");
        final Path ddevConfig = findFile(projectRoot, docroot, "wp-config-ddev.php");

        if (config == null && ddevConfig == null) {
            return null;
        }

        final List<String> contents = new ArrayList<>();
        for (Path file : new Path[]{config, ddevConfig}) {
            if (file != null) {
                contents.add(Files.readString(file));
            }
        }
        final boolean enabled = isTrue(contents, "WP_DEBUG");
        final boolean silent = enabled && isTrue(contents, "WP_DEBUG_LOG") && isFalse(contents, "WP_DEBUG_DISPLAY");
        return new DebugState(enabled, silent);
    }

    public static @NotNull Path setDebugMode(@NotNull Path projectRoot, @NotNull DebugMode mode) throws IOException {
        return setDebugMode(projectRoot, null, mode);
    }

    public static @NotNull Path setDebugMode(@NotNull Path projectRoot, @Nullable String docroot,
                                           @NotNull DebugMode mode) throws IOException {
        final Path config = findFile(projectRoot, docroot, "wp-config.php");

        if (config == null) {
            throw new IOException("No wp-config.php found in " + projectRoot);
        }

        String content = takeOwnership(Files.readString(config));
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

    private static @NotNull String takeOwnership(@NotNull String content) {
        return DDEV_GENERATED_HEADER.matcher(content).replaceAll("");
    }

    private static boolean isTrue(@NotNull List<String> contents, @NotNull String name) {
        final String value = constantValue(contents, name);
        return value != null && "true".equalsIgnoreCase(value.trim());
    }

    private static boolean isFalse(@NotNull List<String> contents, @NotNull String name) {
        final String value = constantValue(contents, name);
        return value != null && "false".equalsIgnoreCase(value.trim());
    }

    private static @Nullable String constantValue(@NotNull List<String> contents, @NotNull String name) {
        for (String content : contents) {
            final Matcher matcher = constantPattern(name).matcher(content);
            if (matcher.find()) {
                return matcher.group("value");
            }
        }
        return null;
    }

    /**
     * Rewrites only the value of an existing definition, so a {@code defined( 'X' ) ||} guard and
     * the surrounding formatting are kept.
     */
    private static @NotNull String setBoolean(@NotNull String content, @NotNull String name, boolean value) {
        final Matcher matcher = constantPattern(name).matcher(content);

        if (matcher.find()) {
            return content.substring(0, matcher.start("value")) + value + content.substring(matcher.end("value"));
        }

        return insertDefinition(content, "define( '" + name + "', " + value + " );");
    }

    private static @NotNull String removeConstant(@NotNull String content, @NotNull String name) {
        return constantPattern(name).matcher(content).replaceAll("")
                .replaceFirst("(?:\\R[\\t ]*){3,}", System.lineSeparator() + System.lineSeparator());
    }

    /**
     * Matches a whole-line definition of the constant, either plain or guarded the way DDEV writes
     * it ({@code defined( 'X' ) || define( 'X', ... );}). The match ends before the line break;
     * the {@code indent} group holds the leading whitespace and {@code value} the defined value.
     */
    static @NotNull Pattern constantPattern(@NotNull String name) {
        final String quotedName = Pattern.quote(name);
        return Pattern.compile("(?m)^(?<indent>[\\t ]*)"
                + "(?:defined\\s*\\(\\s*(['\"])" + quotedName + "\\2\\s*\\)\\s*\\|\\|\\s*)?"
                + "define\\s*\\(\\s*(['\"])" + quotedName + "\\3\\s*,\\s*(?<value>[^;\\r\\n]+?)\\s*\\)\\s*;[\\t ]*(?=\\R|$)");
    }

    private static @NotNull String insertDefinition(@NotNull String content, @NotNull String definition) {
        final String newline = content.contains("\r\n") ? "\r\n" : "\n";
        // Definitions must precede the wp-config-ddev.php include, which follows the "stop editing" line.
        for (Pattern anchor : List.of(Pattern.compile("(?m)^[\\t ]*/\\*\\s+That's all"),
                Pattern.compile("(?m)^[\\t ]*/\\*\\*"))) {
            final Matcher matcher = anchor.matcher(content);
            if (matcher.find()) {
                return content.substring(0, matcher.start()) + definition + newline + newline + content.substring(matcher.start());
            }
        }

        final int phpClose = content.lastIndexOf("?>");
        final int insertionPoint = phpClose >= 0 ? phpClose : content.length();
        final String separator = insertionPoint > 0 && !content.substring(0, insertionPoint).endsWith(newline) ? newline : "";
        return content.substring(0, insertionPoint) + separator + definition + newline + content.substring(insertionPoint);
    }
}
