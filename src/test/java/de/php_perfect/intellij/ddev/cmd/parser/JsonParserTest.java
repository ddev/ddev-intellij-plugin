package de.php_perfect.intellij.ddev.cmd.parser;

import de.php_perfect.intellij.ddev.cmd.Description;
import de.php_perfect.intellij.ddev.cmd.Versions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class JsonParserTest {
    @Test
    void parseValidJson() throws JsonParserException {
        TestObject expected = new TestObject();
        expected.foo = "Bar";

        String json = "{" + "    \"level\": \"info\"," + "    \"msg\": \"Abc\"," + "    \"raw\": {" + "        \"foo\": \"Bar\"" + "    }," + "    \"time\": \"2022-02-05T14:11:53+01:00\"" + "}";

        Assertions.assertEquals(expected, new JsonParserImpl().parse(json, TestObject.class));
    }

    @Test
    void parseInvalidJson() {
        String json = "{]";

        Assertions.assertThrows(JsonParserException.class, () -> new JsonParserImpl().parse(json, TestObject.class));
    }

    @Test
    void parseValidButEmptyJson() {
        String json = "{}";

        Assertions.assertThrows(JsonParserException.class, () -> new JsonParserImpl().parse(json, TestObject.class));
    }

    @Test
    void parseValidJsonWithoutRawProperty() {
        String json = "{" + "    \"level\": \"info\"," + "    \"msg\": \"Abc\"," + "    \"time\": \"2022-02-05T14:11:53+01:00\"" + "}";

        Assertions.assertThrows(JsonParserException.class, () -> new JsonParserImpl().parse(json, TestObject.class));
    }

    @Test
    void statusSuccessfully() throws JsonParserException, IOException {
        String json = Files.readString(Path.of("src/test/resources/ddev_describe.json"));
        Description actual = new JsonParserImpl().parse(json, Description.class);

        Assertions.assertEquals("8.1", actual.getPhpVersion());
    }

    @Test
    void statusWithDebugSuccessfully() throws JsonParserException, IOException {
        String json = Files.readString(Path.of("src/test/resources/ddev_describe_w_debug.json"));
        Description actual = new JsonParserImpl().parse(json, Description.class);

        Assertions.assertEquals("8.1", actual.getPhpVersion());
    }

    @Test
    void parseVersionSuccessfully() throws JsonParserException, IOException {
        String json = Files.readString(Path.of("src/test/resources/ddev_version.json"));
        Versions actual = new JsonParserImpl().parse(json, Versions.class);

        Assertions.assertEquals("v1.19.0", actual.getDdevVersion());
    }

    @Test
    void skipsEmptyLines() throws JsonParserException {
        TestObject expected = new TestObject();
        expected.foo = "Bar";

        Assertions.assertEquals(expected, new JsonParserImpl().parse("\n{\"raw\":{\"foo\":\"Bar\"}}", TestObject.class));
    }
}
