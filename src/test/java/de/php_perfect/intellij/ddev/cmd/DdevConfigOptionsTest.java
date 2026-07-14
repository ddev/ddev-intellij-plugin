package de.php_perfect.intellij.ddev.cmd;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class DdevConfigOptionsTest {
    private static final String SCHEMA = """
            {
              "properties": {
                "type": {"enum": ["generic", "drupal12"]},
                "php_version": {"enum": ["7.4", "8.4", "8.5"]},
                "webserver_type": {"enum": ["nginx-fpm", "apache-fpm", "generic"]},
                "nodejs_version": {"anyOf": [
                  {"enum": ["", "18", "20", "22", "24", "26", "auto", "lts", "latest"]},
                  {"type": "string"}
                ]},
                "database": {
                  "if": {"properties": {"type": {"const": "mariadb"}}},
                  "then": {"properties": {"version": {"enum": ["10.11", "11.8", "12.3"]}}},
                  "else": {
                    "if": {"properties": {"type": {"const": "mysql"}}},
                    "then": {"properties": {"version": {"enum": ["5.7", "8.0", "8.4"]}}},
                    "else": {
                      "if": {"properties": {"type": {"const": "postgres"}}},
                      "then": {"properties": {"version": {"enum": ["16", "17", "18"]}}}
                    }
                  }
                }
              }
            }
            """;

    @Test
    void parsesCurrentConfigurationChoices() {
        final DdevConfigOptions options = DdevConfigOptions.parse(SCHEMA);

        assertThat(options.projectTypes()).containsExactly("generic", "drupal12");
        assertThat(options.phpVersions()).containsExactly("8.5", "8.4", "7.4");
        assertThat(options.nodejsVersions()).containsExactly("26", "24", "22", "20", "18", "auto", "lts", "latest");
        assertThat(options.webserverTypes()).containsExactly("nginx-fpm", "apache-fpm", "generic");
        assertThat(options.databases()).containsExactly(
                "mariadb:12.3", "mariadb:11.8", "mariadb:10.11",
                "mysql:8.4", "mysql:8.0", "mysql:5.7",
                "postgres:18", "postgres:17", "postgres:16");
    }

    @Test
    void fallbackTracksNewDdevOptions() {
        final DdevConfigOptions options = DdevConfigOptions.fallback();

        assertThat(options.phpVersions()).contains("8.5");
        assertThat(options.nodejsVersions()).contains("26");
        assertThat(options.projectTypes()).contains("asterios", "drupal6", "drupal12");
        assertThat(options.databases()).contains("mariadb:12.3", "postgres:18");
    }
}
