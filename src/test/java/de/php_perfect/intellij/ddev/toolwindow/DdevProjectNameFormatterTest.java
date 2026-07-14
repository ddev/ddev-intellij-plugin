package de.php_perfect.intellij.ddev.toolwindow;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class DdevProjectNameFormatterTest {
    @Test
    void supportsTheVscodeProjectNameStyles() {
        assertThat(DdevProjectNameFormatter.format("my-ddev-project", DdevProjectNameFormatter.DEFAULT))
                .isEqualTo("my-ddev-project");
        assertThat(DdevProjectNameFormatter.format("my-ddev-project", DdevProjectNameFormatter.SPACES))
                .isEqualTo("my ddev project");
        assertThat(DdevProjectNameFormatter.format("my-ddev-project", DdevProjectNameFormatter.SENTENCE_CASE))
                .isEqualTo("My ddev project");
        assertThat(DdevProjectNameFormatter.format("my-ddev-project", DdevProjectNameFormatter.TITLE_CASE))
                .isEqualTo("My Ddev Project");
    }

    @Test
    void unknownPersistedStyleFallsBackToTheOriginalName() {
        assertThat(DdevProjectNameFormatter.format("my-project", "Removed style")).isEqualTo("my-project");
    }
}
