package de.php_perfect.intellij.ddev.cmd.parser;

import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public interface JsonParser {
    <T> @NotNull T parse(final String json, final Type typeOfT) throws JsonParserException;

    static JsonParser getInstance() {
        return ApplicationManager.getApplication().getService(JsonParser.class);
    }
}
