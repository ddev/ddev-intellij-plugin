package de.php_perfect.intellij.ddev.cmd;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DdevProjectTypeDetector {
    private static final Pattern TYPE = Pattern.compile("(?m)^type\\s*:\\s*['\"]?([^'\"#\\s]+)");

    private DdevProjectTypeDetector() {
    }

    public static @Nullable String detect(@NotNull String workingDirectory) {
        final Path config = Path.of(workingDirectory, ".ddev", "config.yaml");

        try {
            final Matcher matcher = TYPE.matcher(Files.readString(config));
            return matcher.find() ? matcher.group(1) : null;
        } catch (IOException exception) {
            return null;
        }
    }
}
