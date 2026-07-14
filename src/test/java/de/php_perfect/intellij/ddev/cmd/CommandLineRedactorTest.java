package de.php_perfect.intellij.ddev.cmd;

import com.intellij.execution.configurations.GeneralCommandLine;
import de.php_perfect.intellij.ddev.database.DataSourceConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class CommandLineRedactorTest {
    @Test
    void redactsInlineAndFollowingCredentials() {
        final GeneralCommandLine commandLine = new GeneralCommandLine("ddev", "wp", "core", "install",
                "--admin_password=correct horse battery staple", "--token", "secret-token",
                "--url=https://user:repository-secret@example.com/packages");

        final String description = CommandLineRedactor.describe(commandLine);

        assertThat(description).contains("--admin_password=<redacted>", "--token <redacted>")
                .contains("https://user:<redacted>@example.com/packages")
                .doesNotContain("correct horse", "secret-token", "repository-secret");
    }

    @Test
    void databaseDiagnosticsNeverContainThePassword() {
        final DatabaseInfo info = new DatabaseInfo(DatabaseInfo.Type.MARIADB, "10.11", 3306,
                "db", "db", "db", "super-secret", 32770);

        assertThat(info.toString()).contains("password='<redacted>'").doesNotContain("super-secret");

        final DataSourceConfig dataSource = new DataSourceConfig("project", "DDEV", DataSourceConfig.Type.MARIADB,
                "10.11", "127.0.0.1", 32770, "db", "db", "super-secret");
        assertThat(dataSource.toString()).contains("password='<redacted>'").doesNotContain("super-secret");
    }
}
