package de.php_perfect.intellij.ddev.cms;

import com.networknt.schema.InputFormat;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.Dialects;

import java.nio.file.Files;
import java.nio.file.Path;

public final class RecipeSchemaValidationMain {
    private RecipeSchemaValidationMain() {
    }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 2) {
            throw new IllegalArgumentException("Expected schema and catalog paths");
        }
        final String schemaDocument = Files.readString(Path.of(arguments[0]));
        final String catalogDocument = Files.readString(Path.of(arguments[1]));
        final var errors = SchemaRegistry.withDialect(Dialects.getDraft202012())
                .getSchema(schemaDocument, InputFormat.JSON)
                .validate(catalogDocument, InputFormat.JSON);
        if (!errors.isEmpty()) {
            throw new IllegalStateException("CMS recipe schema validation failed:\n"
                    + String.join("\n", errors.stream().map(Object::toString).toList()));
        }
    }
}
