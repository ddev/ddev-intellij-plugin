package de.php_perfect.intellij.ddev.wordpress;

import org.jetbrains.annotations.NotNull;

public enum WordPressImportPolicy {
    ASK("Ask"),
    ALWAYS("Always"),
    NEVER("Never");

    private final @NotNull String value;

    WordPressImportPolicy(@NotNull String value) {
        this.value = value;
    }

    public @NotNull String value() {
        return this.value;
    }

    public static @NotNull WordPressImportPolicy fromValue(String value) {
        for (WordPressImportPolicy policy : values()) {
            if (policy.value.equals(value)) {
                return policy;
            }
        }

        return ASK;
    }
}
