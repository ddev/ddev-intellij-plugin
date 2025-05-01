package de.php_perfect.intellij.ddev.version;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Version implements Comparable<Version> {

    public final int @NotNull [] numbers;

    public final @NotNull String string;

    /**
     * The build information for head versions (e.g., "36-ge74e3a95f" in "v1.24.4-36-ge74e3a95f")
     * Will be null for regular release versions
     */
    @Nullable
    private final String buildInfo;

    /**
     * Pattern to match head versions like "v1.24.4-36-ge74e3a95f"
     * Group 1: The semantic version part (v1.24.4)
     * Group 2: The build info part (36-ge74e3a95f)
     */
    private static final Pattern HEAD_VERSION_PATTERN = Pattern.compile("^(v?\\d+(?:\\.\\d+)?(?:\\.\\d+)?)-(\\d+-g[a-f0-9]+)$");

    public Version(@NotNull String version) {
        this.string = version;

        // Check if this is a head version
        Matcher headVersionMatcher = HEAD_VERSION_PATTERN.matcher(version);
        String versionForParsing;

        if (headVersionMatcher.matches()) {
            // This is a head version, use the semantic version part for parsing
            versionForParsing = headVersionMatcher.group(1);
            this.buildInfo = headVersionMatcher.group(2);
        } else {
            // Regular version handling
            versionForParsing = version.split("-")[0];
            this.buildInfo = null;
        }

        final String[] split = versionForParsing.replaceAll("^v", "").split("\\.");
        numbers = new int[split.length];
        for (int i = 0; i < split.length; i++) {
            numbers[i] = Integer.parseInt(split[i]);
        }
    }

    public boolean isHeadVersion() {
        return buildInfo != null;
    }

    @Nullable
    public String getBuildInfo() {
        return buildInfo;
    }

    @Override
    public int compareTo(@NotNull Version another) {
        final int maxLength = Math.max(numbers.length, another.numbers.length);
        for (int i = 0; i < maxLength; i++) {
            final int left = i < numbers.length ? numbers[i] : 0;
            final int right = i < another.numbers.length ? another.numbers[i] : 0;
            if (left != right) {
                return left < right ? -1 : 1;
            }
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Version version = (Version) o;
        return Arrays.equals(numbers, version.numbers);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(numbers);
    }

    @Override
    public String toString() {
        return this.string;
    }
}
