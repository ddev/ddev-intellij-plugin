package de.php_perfect.intellij.ddev.toolwindow;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class DdevProjectNameFormatter {
    public static final String DEFAULT = "Default";
    public static final String SPACES = "Spaces";
    public static final String SENTENCE_CASE = "Sentence case";
    public static final String TITLE_CASE = "Title Case";

    private DdevProjectNameFormatter() {
    }

    public static @NotNull String format(@NotNull String name, @NotNull String format) {
        if (DEFAULT.equals(format)) {
            return name;
        }

        final String spaced = name.replace('-', ' ');

        if (SPACES.equals(format) || spaced.isEmpty()) {
            return spaced;
        }

        if (SENTENCE_CASE.equals(format)) {
            return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
        }

        if (TITLE_CASE.equals(format)) {
            return Arrays.stream(spaced.split(" ", -1))
                    .map(word -> word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1))
                    .collect(Collectors.joining(" "));
        }

        return name;
    }
}
