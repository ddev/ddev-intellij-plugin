package de.php_perfect.intellij.ddev.cmd.parser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Utility class for parsing JSON.
 */
public final class JsonUtil {
    private static final Logger LOG = Logger.getInstance(JsonUtil.class);

    private JsonUtil() {
        // Utility class
    }

    /**
     * Parses a JSON string into a Map.
     *
     * @param json The JSON string to parse
     * @return A Map representing the JSON object, or null if parsing fails
     */
    @Nullable
    public static Map<String, Object> parseJson(@NotNull String json) {
        try {
            JsonElement jsonElement = JsonParser.parseString(json);
            if (jsonElement.isJsonObject()) {
                return convertJsonObjectToMap(jsonElement.getAsJsonObject());
            } else {
                LOG.warn("JSON is not an object: " + json);
                return null;
            }
        } catch (JsonSyntaxException e) {
            LOG.error("Failed to parse JSON: " + json, e);
            return null;
        }
    }

    /**
     * Converts a JsonObject to a Map.
     *
     * @param jsonObject The JsonObject to convert
     * @return A Map representing the JsonObject
     */
    private static Map<String, Object> convertJsonObjectToMap(JsonObject jsonObject) {
        Map<String, Object> map = new HashMap<>();
        for (Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            map.put(key, convertJsonElementToObject(value));
        }
        return map;
    }

    /**
     * Converts a JsonElement to a Java object.
     *
     * @param jsonElement The JsonElement to convert
     * @return A Java object representing the JsonElement
     */
    private static Object convertJsonElementToObject(JsonElement jsonElement) {
        if (jsonElement.isJsonNull()) {
            return null;
        } else if (jsonElement.isJsonPrimitive()) {
            JsonPrimitive primitive = jsonElement.getAsJsonPrimitive();
            if (primitive.isBoolean()) {
                return primitive.getAsBoolean();
            } else if (primitive.isNumber()) {
                return primitive.getAsNumber();
            } else {
                return primitive.getAsString();
            }
        } else if (jsonElement.isJsonArray()) {
            List<Object> list = new ArrayList<>();
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            for (JsonElement element : jsonArray) {
                list.add(convertJsonElementToObject(element));
            }
            return list;
        } else if (jsonElement.isJsonObject()) {
            return convertJsonObjectToMap(jsonElement.getAsJsonObject());
        } else {
            return null;
        }
    }

    /**
     * Gets a list of maps from a JSON object.
     *
     * @param json The JSON object as a Map
     * @param key  The key of the array in the JSON object
     * @return A list of maps representing the JSON array, or an empty list if the key is not found
     */
    @NotNull
    public static List<Map<String, Object>> getList(@Nullable Map<String, Object> json, @NotNull String key) {
        if (json == null) {
            return new ArrayList<>();
        }

        Object value = json.get(key);
        if (value instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> typedMap = (Map<String, Object>) map;
                    result.add(typedMap);
                }
            }
            return result;
        }

        return new ArrayList<>();
    }

    /**
     * Gets a string value from a JSON object.
     *
     * @param json The JSON object as a Map
     * @param key  The key of the string in the JSON object
     * @return The string value, or an empty string if the key is not found
     */
    @NotNull
    public static String getString(@Nullable Map<String, Object> json, @NotNull String key) {
        if (json == null) {
            return "";
        }

        Object value = json.get(key);
        if (value instanceof String string) {
            return string;
        }

        return value != null ? value.toString() : "";
    }

    /**
     * Gets a boolean value from a JSON object.
     *
     * @param json The JSON object as a Map
     * @param key  The key of the boolean in the JSON object
     * @return The boolean value, or false if the key is not found
     */
    public static boolean getBoolean(@Nullable Map<String, Object> json, @NotNull String key) {
        if (json == null) {
            return false;
        }

        Object value = json.get(key);
        if (value instanceof Boolean boolValue) {
            return boolValue;
        }

        return false;
    }

    /**
     * Gets an integer value from a JSON object.
     *
     * @param json The JSON object as a Map
     * @param key  The key of the integer in the JSON object
     * @return The integer value, or 0 if the key is not found
     */
    public static int getInt(@Nullable Map<String, Object> json, @NotNull String key) {
        if (json == null) {
            return 0;
        }

        Object value = json.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }

        return 0;
    }

    /**
     * Gets a map value from a JSON object.
     *
     * @param json The JSON object as a Map
     * @param key  The key of the map in the JSON object
     * @return The map value, or null if the key is not found
     */
    @Nullable
    public static Map<String, Object> getMap(@Nullable Map<String, Object> json, @NotNull String key) {
        if (json == null) {
            return null;
        }

        Object value = json.get(key);
        if (value instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> typedMap = (Map<String, Object>) map;
            return typedMap;
        }

        return null;
    }
}
