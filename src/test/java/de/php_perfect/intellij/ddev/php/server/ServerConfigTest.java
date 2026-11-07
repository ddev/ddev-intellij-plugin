package de.php_perfect.intellij.ddev.php.server;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;

final class ServerConfigTest {
    private final ServerConfig config = new ServerConfig("/local", "/var/www/html", URI.create("https://acol.ddev.site"));

    @Test
    void equalConfigsMatch() {
        final ServerConfig other = new ServerConfig("/local", "/var/www/html", URI.create("https://acol.ddev.site"));

        Assertions.assertEquals(this.config, this.config);
        Assertions.assertEquals(this.config, other);
        Assertions.assertEquals(this.config.hashCode(), other.hashCode());
    }

    @Test
    void differingConfigsDoNotMatch() {
        Assertions.assertNotEquals(null, this.config);
        Assertions.assertNotEquals(new Object(), this.config);
        Assertions.assertNotEquals(new ServerConfig("/other", "/var/www/html", URI.create("https://acol.ddev.site")), this.config);
        Assertions.assertNotEquals(new ServerConfig("/local", "/srv", URI.create("https://acol.ddev.site")), this.config);
        Assertions.assertNotEquals(new ServerConfig("/local", "/var/www/html", URI.create("https://other.ddev.site")), this.config);
    }

    @Test
    void toStringListsFields() {
        Assertions.assertEquals("ServerConfig{localPath='/local', remotePathPath='/var/www/html', uri=https://acol.ddev.site}", this.config.toString());
    }
}
