package de.php_perfect.intellij.ddev.node;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class NodeInterpreterConfigTest {
    private final NodeInterpreterConfig config = new NodeInterpreterConfig("acol", "/compose.yaml", "node");

    @Test
    void equalConfigsMatch() {
        final NodeInterpreterConfig other = new NodeInterpreterConfig("acol", "/compose.yaml", "node");

        Assertions.assertEquals(this.config, this.config);
        Assertions.assertEquals(this.config, other);
        Assertions.assertEquals(this.config.hashCode(), other.hashCode());
    }

    @Test
    void differingConfigsDoNotMatch() {
        Assertions.assertNotEquals(null, this.config);
        Assertions.assertNotEquals("acol", this.config);
        Assertions.assertNotEquals(new NodeInterpreterConfig("other", "/compose.yaml", "node"), this.config);
        Assertions.assertNotEquals(new NodeInterpreterConfig("acol", "/other.yaml", "node"), this.config);
        Assertions.assertNotEquals(new NodeInterpreterConfig("acol", "/compose.yaml", "nodejs"), this.config);
    }

    @Test
    void toStringListsFields() {
        Assertions.assertEquals("NodeInterpreterConfig{name='acol', composeFilePath='/compose.yaml', binaryPath='node'}", this.config.toString());
    }
}
