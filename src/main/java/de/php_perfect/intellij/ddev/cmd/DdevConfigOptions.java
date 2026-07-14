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

    static @NotNull DdevConfigOptions parseSnapshot(@NotNull String json) {
        final JsonObject snapshot = JsonParser.parseString(json).getAsJsonObject();
        return new DdevConfigOptions(
                arrayValues(snapshot, "projectTypes"),
                arrayValues(snapshot, "phpVersions"),
                arrayValues(snapshot, "nodejsVersions"),
                arrayValues(snapshot, "webserverTypes"),
                arrayValues(snapshot, "databases")
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

    private static @NotNull List<String> arrayValues(@NotNull JsonObject object, @NotNull String name) {
        final JsonArray values = object.getAsJsonArray(name);
        final List<String> result = new ArrayList<>(values.size());

        for (final JsonElement value : values) {
            result.add(value.getAsString());
        }

        return result;
    }
}
