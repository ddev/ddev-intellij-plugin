package de.php_perfect.intellij.ddev.cmd;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Configuration choices published by DDEV's project configuration schema. */
public record DdevConfigOptions(
        @NotNull List<String> projectTypes,
        @NotNull List<String> phpVersions,
        @NotNull List<String> nodejsVersions,
        @NotNull List<String> webserverTypes,
        @NotNull List<String> databases
) {
    public DdevConfigOptions {
        projectTypes = List.copyOf(projectTypes);
        phpVersions = List.copyOf(phpVersions);
        nodejsVersions = List.copyOf(nodejsVersions);
        webserverTypes = List.copyOf(webserverTypes);
        databases = List.copyOf(databases);
    }

    static @NotNull DdevConfigOptions parse(@NotNull String json) {
        final JsonObject properties = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("properties");
        final List<String> projectTypes = enumValues(properties.getAsJsonObject("type"));
        final List<String> phpVersions = enumValues(properties.getAsJsonObject("php_version")).reversed();
        final List<String> webserverTypes = enumValues(properties.getAsJsonObject("webserver_type"));
        final List<String> nodejsVersions = parseNodejsVersions(properties.getAsJsonObject("nodejs_version"));
        final List<String> databases = parseDatabases(properties.getAsJsonObject("database"));

        return new DdevConfigOptions(projectTypes, phpVersions, nodejsVersions, webserverTypes, databases);
    }

    static @NotNull DdevConfigOptions fallback() {
        return new DdevConfigOptions(
                List.of("asterios", "backdrop", "cakephp", "codeigniter", "craftcms", "drupal", "drupal6",
                        "drupal7", "drupal8", "drupal9", "drupal10", "drupal11", "drupal12", "generic",
                        "joomla", "laravel", "magento", "magento2", "php", "shopware6", "silverstripe",
                        "symfony", "typo3", "wordpress", "wp-bedrock"),
                List.of("8.5", "8.4", "8.3", "8.2", "8.1", "8.0", "7.4", "7.3", "7.2", "7.1", "7.0", "5.6"),
                List.of("26", "24", "22", "20", "18", "16", "14", "12", "10", "8", "6", "auto", "lts", "latest", "current", "engine", "nightly"),
                List.of("nginx-fpm", "apache-fpm", "generic"),
                List.of("mariadb:12.3", "mariadb:11.8", "mariadb:11.4", "mariadb:10.11", "mariadb:10.8",
                        "mariadb:10.7", "mariadb:10.6", "mariadb:10.5", "mariadb:10.4", "mariadb:10.3",
                        "mariadb:10.2", "mariadb:10.1", "mariadb:10.0", "mariadb:5.5", "mysql:8.4",
                        "mysql:8.0", "mysql:5.7", "mysql:5.6", "mysql:5.5", "postgres:18", "postgres:17",
                        "postgres:16", "postgres:15", "postgres:14", "postgres:13", "postgres:12",
                        "postgres:11", "postgres:10", "postgres:9")
        );
    }

    private static @NotNull List<String> parseNodejsVersions(@Nullable JsonObject nodejs) {
        if (nodejs == null || !nodejs.has("anyOf")) {
            return List.of();
        }

        for (final JsonElement choice : nodejs.getAsJsonArray("anyOf")) {
            if (choice.isJsonObject() && choice.getAsJsonObject().has("enum")) {
                final List<String> values = enumValues(choice.getAsJsonObject()).stream()
                        .filter(value -> !value.isBlank())
                        .toList();
                final List<String> result = new ArrayList<>();
                result.addAll(values.stream().filter(value -> value.chars().allMatch(Character::isDigit)).toList().reversed());
                result.addAll(values.stream().filter(value -> !value.chars().allMatch(Character::isDigit)).toList());
                return result;
            }
        }

        return List.of();
    }

    private static @NotNull List<String> parseDatabases(@Nullable JsonObject database) {
        if (database == null) {
            return List.of();
        }

        final List<String> result = new ArrayList<>();
        JsonObject branch = database;

        while (branch != null && branch.has("if") && branch.has("then")) {
            final JsonObject conditionProperties = branch.getAsJsonObject("if").getAsJsonObject("properties");
            final JsonObject typeCondition = conditionProperties != null ? conditionProperties.getAsJsonObject("type") : null;
            final String type = typeCondition != null && typeCondition.has("const")
                    ? typeCondition.get("const").getAsString()
                    : null;
            final JsonObject thenProperties = branch.getAsJsonObject("then").getAsJsonObject("properties");
            final JsonObject version = thenProperties != null ? thenProperties.getAsJsonObject("version") : null;

            if (type != null) {
                for (final String value : enumValues(version).reversed()) {
                    result.add(type + ":" + value);
                }
            }

            branch = branch.has("else") && branch.get("else").isJsonObject()
                    ? branch.getAsJsonObject("else")
                    : null;
        }

        return result;
    }

    private static @NotNull List<String> enumValues(@Nullable JsonObject property) {
        if (property == null || !property.has("enum")) {
            return List.of();
        }

        final JsonArray values = property.getAsJsonArray("enum");
        final List<String> result = new ArrayList<>(values.size());

        for (final JsonElement value : values) {
            result.add(value.getAsString());
        }

        return result;
    }
}
