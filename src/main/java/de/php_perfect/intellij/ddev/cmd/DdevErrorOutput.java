package de.php_perfect.intellij.ddev.cmd;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Extracts the error DDEV reports on stderr. With --json-output every log line is a JSON object
 * such as {"level":"fatal","msg":"...","time":"..."}; without it the plain text is used as is.
 */
public final class DdevErrorOutput {
    private static final Set<String> ERROR_LEVELS = Set.of("fatal", "error");

    private DdevErrorOutput() {
    }

    public static @Nullable String extractMessage(@NotNull String stderr) {
        String message = null;
        final StringBuilder plainText = new StringBuilder();

        for (String line : stderr.split("\\R")) {
            final String trimmed = line.strip();
            if (trimmed.isEmpty()) {
                continue;
            }

            final JsonObject entry = parseObject(trimmed);
            if (entry == null) {
                plainText.append(plainText.isEmpty() ? "" : "\n").append(trimmed);
                continue;
            }

            final String level = stringMember(entry, "level");
            final String msg = stringMember(entry, "msg");
            if (msg != null && level != null && ERROR_LEVELS.contains(level)) {
                message = msg;
            }
        }

        if (message != null) {
            return message;
        }

        return plainText.isEmpty() ? null : plainText.toString();
    }

    private static @Nullable JsonObject parseObject(@NotNull String line) {
        if (!line.startsWith("{")) {
            return null;
        }

        try {
            final JsonElement element = JsonParser.parseString(line);
            return element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (JsonParseException exception) {
            return null;
        }
    }

    private static @Nullable String stringMember(@NotNull JsonObject object, @NotNull String name) {
        final JsonElement element = object.get(name);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }
}
