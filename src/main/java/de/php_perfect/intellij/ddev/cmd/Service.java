package de.php_perfect.intellij.ddev.cmd;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class Service {
    private final @Nullable String fullName;

    private final @Nullable String httpsUrl;

    private final @Nullable String httpUrl;

    public Service(@Nullable String fullName, @Nullable String httpsUrl, @Nullable String httpUrl) {
        this.fullName = fullName;
        this.httpsUrl = httpsUrl;
        this.httpUrl = httpUrl;
    }

    public @Nullable String getFullName() {
        return fullName;
    }

    public @Nullable String getHttpUrl() {
        return httpUrl;
    }

    public @Nullable String getHttpsUrl() {
        return httpsUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Service service)) return false;
        return Objects.equals(getFullName(), service.getFullName()) && Objects.equals(getHttpUrl(), service.getHttpUrl()) && Objects.equals(getHttpsUrl(), service.getHttpsUrl());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFullName(), getHttpUrl(), getHttpsUrl());
    }

    @Override
    public String toString() {
        return "Service{" +
                "fullName='" + fullName + '\'' +
                ", httpUrl='" + httpUrl + '\'' +
                ", httpsUrl='" + httpsUrl + '\'' +
                '}';
    }
}
