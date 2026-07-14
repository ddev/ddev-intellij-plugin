package de.php_perfect.intellij.ddev.cmd;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record DatabaseInfo(
        @SerializedName("database_type") de.php_perfect.intellij.ddev.cmd.DatabaseInfo.@Nullable Type type,
        @SerializedName("database_version") @Nullable String version, @SerializedName("dbPort") int port,
        @SerializedName("dbname") @Nullable String name, @SerializedName("host") @Nullable String host,
        @SerializedName("username") @Nullable String username, @SerializedName("password") @Nullable String password,
        @SerializedName("published_port") int publishedPort) {
    public enum Type {
        @SerializedName("mysql") MYSQL,
        @SerializedName("mariadb") MARIADB,
        @SerializedName("postgres") POSTGRESQL,
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DatabaseInfo(
                Type type1, String version1, int port1, String name1, String host1, String username1, String password1,
                int publishedPort1
        ))) return false;
        return port == port1 && publishedPort == publishedPort1 && Objects.equals(name, name1) && Objects.equals(host, host1) && Objects.equals(version, version1) && Objects.equals(username, username1) && Objects.equals(password, password1) && type == type1;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, version, port, name, host, username, password, publishedPort);
    }

    @Override
    public String toString() {
        return "DatabaseInfo{" +
                "type=" + type +
                ", version='" + version + '\'' +
                ", port=" + port +
                ", name='" + name + '\'' +
                ", host='" + host + '\'' +
                ", username='" + username + '\'' +
                ", password='<redacted>'" +
                ", publishedPort=" + publishedPort +
                '}';
    }
}
