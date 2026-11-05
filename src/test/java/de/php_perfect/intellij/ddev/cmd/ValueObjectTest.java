package de.php_perfect.intellij.ddev.cmd;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

final class ValueObjectTest {
    @Test
    void serviceEquality() {
        final Service service = new Service("ddev-acol-web", "https://acol.ddev.site", "http://acol.ddev.site");

        assertEqualityContract(service, new Service("ddev-acol-web", "https://acol.ddev.site", "http://acol.ddev.site"), List.of(
                new Service("ddev-acol-db", "https://acol.ddev.site", "http://acol.ddev.site"),
                new Service("ddev-acol-web", "https://acol.ddev.site", "http://other.ddev.site"),
                new Service("ddev-acol-web", "https://other.ddev.site", "http://acol.ddev.site")
        ));
        Assertions.assertEquals("Service{fullName='ddev-acol-web', httpUrl='http://acol.ddev.site', httpsUrl='https://acol.ddev.site'}", service.toString());
    }

    @Test
    void versionsEquality() {
        final Versions versions = new Versions("v1.23.0", "24.0.7", "v2.23.3", "docker-desktop");

        assertEqualityContract(versions, new Versions("v1.23.0", "24.0.7", "v2.23.3", "docker-desktop"), List.of(
                new Versions("v1.22.0", "24.0.7", "v2.23.3", "docker-desktop"),
                new Versions("v1.23.0", "20.10.12", "v2.23.3", "docker-desktop"),
                new Versions("v1.23.0", "24.0.7", "v2.2.2", "docker-desktop"),
                new Versions("v1.23.0", "24.0.7", "v2.23.3", "colima")
        ));
        Assertions.assertEquals("Versions{ddevVersion='v1.23.0', dockerVersion='24.0.7', dockerComposeVersion='v2.23.3', dockerPlatform='docker-desktop'}", versions.toString());
    }

    @Test
    void databaseInfoEquality() {
        final DatabaseInfo databaseInfo = new DatabaseInfo(DatabaseInfo.Type.MYSQL, "8.0", 3306, "db", "db", "user", "secret", 32768);

        assertEqualityContract(databaseInfo, new DatabaseInfo(DatabaseInfo.Type.MYSQL, "8.0", 3306, "db", "db", "user", "secret", 32768), List.of(
                new DatabaseInfo(DatabaseInfo.Type.MYSQL, "8.0", 3307, "db", "db", "user", "secret", 32768),
                new DatabaseInfo(DatabaseInfo.Type.MYSQL, "8.0", 3306, "db", "db", "user", "secret", 32769),
                new DatabaseInfo(DatabaseInfo.Type.MYSQL, "8.0", 3306, "other", "db", "user", "secret", 32768),
                new DatabaseInfo(DatabaseInfo.Type.MYSQL, "8.0", 3306, "db", "localhost", "user", "secret", 32768),
                new DatabaseInfo(DatabaseInfo.Type.MYSQL, "5.7", 3306, "db", "db", "user", "secret", 32768),
                new DatabaseInfo(DatabaseInfo.Type.MYSQL, "8.0", 3306, "db", "db", "root", "secret", 32768),
                new DatabaseInfo(DatabaseInfo.Type.MYSQL, "8.0", 3306, "db", "db", "user", "root", 32768),
                new DatabaseInfo(DatabaseInfo.Type.POSTGRESQL, "8.0", 3306, "db", "db", "user", "secret", 32768)
        ));
        Assertions.assertEquals("DatabaseInfo{type=MYSQL, version='8.0', port=3306, name='db', host='db', username='user', password='secret', publishedPort=32768}", databaseInfo.toString());
    }

    @Test
    void commandFailedExceptionCarriesDdevMessage() {
        final CommandFailedException exception = new CommandFailedException("ddev describe failed", "Docker is not running");

        Assertions.assertEquals("ddev describe failed", exception.getMessage());
        Assertions.assertEquals("Docker is not running", exception.getDdevMessage());
        Assertions.assertNull(new CommandFailedException("ddev describe failed").getDdevMessage());
    }

    private static void assertEqualityContract(Object value, Object equalValue, List<?> differentValues) {
        Assertions.assertEquals(value, value);
        Assertions.assertEquals(value, equalValue);
        Assertions.assertEquals(value.hashCode(), equalValue.hashCode());
        Assertions.assertNotEquals(null, value);
        Assertions.assertNotEquals(value, "other");

        for (final Object differentValue : differentValues) {
            Assertions.assertNotEquals(value, differentValue, differentValue.toString());
        }
    }
}
