package de.php_perfect.intellij.ddev.database;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class DataSourceConfigTest {
    private final DataSourceConfig config = config("DDEV", "desc", DataSourceConfig.Type.MYSQL, "8.0", "127.0.0.1", 3306, "db", "user", "pass");

    @Test
    void equalConfigsMatch() {
        final DataSourceConfig other = config("DDEV", "desc", DataSourceConfig.Type.MYSQL, "8.0", "127.0.0.1", 3306, "db", "user", "pass");

        Assertions.assertEquals(this.config, this.config);
        Assertions.assertEquals(this.config, other);
        Assertions.assertEquals(this.config.hashCode(), other.hashCode());
    }

    @Test
    void differingConfigsDoNotMatch() {
        Assertions.assertNotEquals(null, this.config);
        Assertions.assertNotEquals("DDEV", this.config);
        Assertions.assertNotEquals(config("DDEV", "desc", DataSourceConfig.Type.MYSQL, "8.0", "127.0.0.1", 3307, "db", "user", "pass"), this.config);
        Assertions.assertNotEquals(config("Other", "desc", DataSourceConfig.Type.MYSQL, "8.0", "127.0.0.1", 3306, "db", "user", "pass"), this.config);
        Assertions.assertNotEquals(config("DDEV", "other", DataSourceConfig.Type.MYSQL, "8.0", "127.0.0.1", 3306, "db", "user", "pass"), this.config);
        Assertions.assertNotEquals(config("DDEV", "desc", DataSourceConfig.Type.MARIADB, "8.0", "127.0.0.1", 3306, "db", "user", "pass"), this.config);
        Assertions.assertNotEquals(config("DDEV", "desc", DataSourceConfig.Type.MYSQL, "5.7", "127.0.0.1", 3306, "db", "user", "pass"), this.config);
        Assertions.assertNotEquals(config("DDEV", "desc", DataSourceConfig.Type.MYSQL, "8.0", "localhost", 3306, "db", "user", "pass"), this.config);
        Assertions.assertNotEquals(config("DDEV", "desc", DataSourceConfig.Type.MYSQL, "8.0", "127.0.0.1", 3306, "other", "user", "pass"), this.config);
        Assertions.assertNotEquals(config("DDEV", "desc", DataSourceConfig.Type.MYSQL, "8.0", "127.0.0.1", 3306, "db", "other", "pass"), this.config);
        Assertions.assertNotEquals(config("DDEV", "desc", DataSourceConfig.Type.MYSQL, "8.0", "127.0.0.1", 3306, "db", "user", "other"), this.config);
    }

    @Test
    void toStringListsFields() {
        Assertions.assertEquals(
                "DataSourceConfig{name='DDEV', description='desc', type=MYSQL, version='8.0', host='127.0.0.1', port=3306, database='db', username='user', password='pass'}",
                this.config.toString()
        );
    }

    private static DataSourceConfig config(String name, String description, DataSourceConfig.Type type, String version, String host, int port, String database, String username, String password) {
        return new DataSourceConfig(name, description, type, version, host, port, database, username, password);
    }
}
