package de.php_perfect.intellij.ddev.wordpress;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class WordPressImportReconcilerTest {
    @Test
    void detectsPrefixOnlyForAWordPressTableSet() {
        assertThat(WordPressImportReconciler.detectPrefix(List.of(
                "client_options", "client_posts", "client_users"))).isEqualTo("client_");
        assertThat(WordPressImportReconciler.detectPrefix(List.of("application_options"))).isNull();
    }

    @Test
    void buildsQuotedPrefixMigration() {
        assertThat(WordPressImportReconciler.buildPrefixRenameSql(
                List.of("client_posts", "client_options", "unrelated"), "client_", "wp_"))
                .contains("RENAME TABLE `client_options` TO `wp_options`")
                .contains("RENAME TABLE `client_posts` TO `wp_posts`")
                .doesNotContain("unrelated")
                .contains("UPDATE `wp_options`")
                .contains("UPDATE `wp_usermeta`");
    }

    @Test
    void rejectsUnsafePrefixes() {
        assertThatThrownBy(() -> WordPressImportReconciler.buildPrefixRenameSql(
                List.of("wp_options"), "wp_; DROP TABLE users", "wp_"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void replacesWordPressHostsAcrossHttpAndHttpsUrls() {
        assertThat(WordPressImportReconciler.buildSearchReplaceArguments(
                "https://www.example.com/", "https://example.ddev.site"))
                .containsExactly("wp", "search-replace", "www.example.com", "example.ddev.site",
                        "--skip-columns=guid", "--all-tables");
    }
}
