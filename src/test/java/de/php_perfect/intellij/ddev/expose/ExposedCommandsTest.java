package de.php_perfect.intellij.ddev.expose;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

final class ExposedCommandsTest {
    @Test
    void parsesCommandNamesAndContainerPaths() {
        assertThat(ExposedCommands.parse("wp")).isEqualTo(new ExposedCommands.Launcher("wp", "wp"));
        assertThat(ExposedCommands.parse("/usr/bin/sqlite3"))
                .isEqualTo(new ExposedCommands.Launcher("sqlite3", "/usr/bin/sqlite3"));
        assertThat(ExposedCommands.parse("./vendor/bin/drush"))
                .isEqualTo(new ExposedCommands.Launcher("drush", "/var/www/html/vendor/bin/drush"));
        assertThat(ExposedCommands.parse("vendor/bin/phpstan"))
                .isEqualTo(new ExposedCommands.Launcher("phpstan", "/var/www/html/vendor/bin/phpstan"));
    }

    @Test
    void rejectsEntriesThatAreNotPlainCommands() {
        for (String entry : List.of("rm;ls", "$(id)", "../wp", "/usr/bin/", "-rf", "a b", "/bin/../etc/x", "")) {
            assertThat(ExposedCommands.parse(entry)).as(entry).isNull();
        }
    }

    @Test
    void projectEntriesReplaceGlobalEntriesOfTheSameName() {
        assertThat(ExposedCommands.launchers(List.of("wp", "drush", "bad;"), List.of("/opt/wp-cli/wp", "npm")))
                .containsExactly(
                        new ExposedCommands.Launcher("wp", "/opt/wp-cli/wp"),
                        new ExposedCommands.Launcher("drush", "drush"),
                        new ExposedCommands.Launcher("npm", "npm"));
    }
}
