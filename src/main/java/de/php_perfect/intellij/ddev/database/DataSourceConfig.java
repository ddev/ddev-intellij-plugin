package de.php_perfect.intellij.ddev.database;

import de.php_perfect.intellij.ddev.index.IndexableConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record DataSourceConfig(@NotNull String name, @NotNull String description, @NotNull Type type,
                               @NotNull String version, @NotNull String host, int port, @NotNull String database,
                               @NotNull String username, @NotNull String password) implements IndexableConfiguration {
    public enum Type {
        MYSQL,
        MARIADB,
        POSTGRESQL,
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataSourceConfig that = (DataSourceConfig) o;
        return port == that.port && Objects.equals(name, that.name) && Objects.equals(description, that.description) && type == that.type && Objects.equals(version, that.version) && Objects.equals(host, that.host) && Objects.equals(database, that.database) && Objects.equals(username, that.username) && Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, type, version, host, port, database, username, password);
    }

    @Override
    public String toString() {
        return "DataSourceConfig{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", type=" + type +
                ", version='" + version + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", database='" + database + '\'' +
                ", username='" + username + '\'' +
                ", password='<redacted>'" +
                '}';
    }
}
