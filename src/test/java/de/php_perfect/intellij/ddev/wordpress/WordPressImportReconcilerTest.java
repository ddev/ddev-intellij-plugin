package de.php_perfect.intellij.ddev.wordpress;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.cmd.CommandFailedException;
import de.php_perfect.intellij.ddev.cmd.ProcessExecutor;
import de.php_perfect.intellij.ddev.cmd.Runner;
import de.php_perfect.intellij.ddev.settings.DdevSettingsState;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

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

    @Test
    void readsEvaluatedPrefixAndRejectsUnusableOutput() throws Exception {
        final ProcessExecutor executor = mock(ProcessExecutor.class);
        when(executor.executeCommandLine(any(), anyInt(), eq(false)))
                .thenReturn(new ProcessOutput("client_\n", "", 0, false, false))
                .thenReturn(new ProcessOutput("unexpected output\nclient_", "", 0, false, false))
                .thenReturn(new ProcessOutput("", "config error", 1, false, false));
        try (var service = mockStatic(ProcessExecutor.class)) {
            service.when(ProcessExecutor::getInstance).thenReturn(executor);
            final Project project = mock(Project.class);
            assertThat(WordPressImportReconciler.configuredPrefix(project, "/custom/ddev", "/project"))
                    .isEqualTo("client_");
            for (int attempt = 0; attempt < 2; attempt++) {
                assertThatThrownBy(() -> WordPressImportReconciler.configuredPrefix(project, "/custom/ddev", "/project"))
                        .isInstanceOf(CommandFailedException.class);
            }
            final var command = ArgumentCaptor.forClass(GeneralCommandLine.class);
            verify(executor, times(3)).executeCommandLine(command.capture(), anyInt(), eq(false));
            assertThat(command.getValue().getExePath()).isEqualTo("/custom/ddev");
            assertThat(command.getValue().getParametersList().getList().get(1))
                    .contains("after_wp_config_load", "$GLOBALS['table_prefix']");
        }
    }

    @Test
    void preservesMatchingCustomPrefixAndMigratesOnlyToTheConfiguredPrefix() {
        final Project project = mock(Project.class);
        final DdevSettingsState settings = new DdevSettingsState();
        settings.wordpressTablePrefixImportPolicy = "Always";
        final Runner runner = mock(Runner.class);
        when(project.getService(DdevSettingsState.class)).thenReturn(settings);
        when(project.getService(Runner.class)).thenReturn(runner);
        final List<String> tables = List.of("client_options", "client_posts", "client_users", "client_usermeta");
        final String url = "https://local.ddev.site";
        WordPressImportReconciler.reconcileOnEdt(project, "ddev", "/project", tables, "client_", "client_", url, url);
        final var command = ArgumentCaptor.forClass(GeneralCommandLine.class);
        verify(runner).runOnSuccess(command.capture(), anyString(), isNull());
        assertThat(command.getValue().getParametersList().getList()).containsExactly("wp", "rewrite", "flush");

        clearInvocations(runner);
        WordPressImportReconciler.reconcileOnEdt(project, "ddev", "/project", tables, "client_", "site_", url, url);
        verify(runner).runOnSuccess(command.capture(), anyString(), any(Runnable.class));
        assertThat(command.getValue().getParametersList().getList().getLast())
                .contains("RENAME TABLE `client_options` TO `site_options`").doesNotContain("`wp_");

        clearInvocations(runner);
        settings.wordpressTablePrefixImportPolicy = "Never";
        WordPressImportReconciler.reconcileOnEdt(project, "ddev", "/project", tables, "client_", "site_", url, url);
        verifyNoInteractions(runner);
    }
}
