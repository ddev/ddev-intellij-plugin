package de.php_perfect.intellij.ddev.php;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class DdevInterpreterConfigTest {
    private final DdevInterpreterConfig config = new DdevInterpreterConfig("acol", "php8.1", "/compose.yaml");

    @Test
    void equalConfigsMatch() {
        final DdevInterpreterConfig other = new DdevInterpreterConfig("acol", "php8.1", "/compose.yaml");

        Assertions.assertEquals(this.config, this.config);
        Assertions.assertEquals(this.config, other);
        Assertions.assertEquals(this.config.hashCode(), other.hashCode());
    }

    @Test
    void differingConfigsDoNotMatch() {
        Assertions.assertNotEquals(null, this.config);
        Assertions.assertNotEquals("acol", this.config);
        Assertions.assertNotEquals(new DdevInterpreterConfig("other", "php8.1", "/compose.yaml"), this.config);
        Assertions.assertNotEquals(new DdevInterpreterConfig("acol", "php7.4", "/compose.yaml"), this.config);
        Assertions.assertNotEquals(new DdevInterpreterConfig("acol", "php8.1", "/other.yaml"), this.config);
    }

    @Test
    void toStringListsFields() {
        Assertions.assertEquals("DdevInterpreterConfig{name='acol', phpVersion='php8.1', composeFilePath='/compose.yaml'}", this.config.toString());
    }
}
