package de.php_perfect.intellij.ddev.cmd;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Description {

    public enum Status {
        @SerializedName("running") RUNNING,
        @SerializedName("starting") STARTING,
        @SerializedName("stopped") STOPPED,
        @SerializedName("project directory missing") DIR_MISSING,
        @SerializedName(".ddev/config.yaml missing") CONFIG_MISSING,
        @SerializedName("paused") PAUSED,
        @SerializedName("unhealthy") UNHEALTHY,
    }

    private final @Nullable String name;

    private final @Nullable String phpVersion;

    private final @Nullable Status status;

    @SerializedName("mailhog_https_url")
    private final @Nullable String mailHogHttpsUrl;

    @SerializedName("mailhog_url")
    private final @Nullable String mailHogHttpUrl;

    private final @Nullable String mailpitHttpsUrl;

    private final @Nullable String mailpitHttpUrl;


    private final @Nullable Map<String, Service> services;

    @SerializedName("dbinfo")
    private final @Nullable DatabaseInfo databaseInfo;

    @SerializedName("primary_url")
    private final @Nullable String primaryUrl;

    // Private constructor for builder
    private Description(Builder builder) {
        this.name = builder.name;
        this.phpVersion = builder.phpVersion;
        this.status = builder.status;
        this.mailHogHttpsUrl = builder.mailHogHttpsUrl;
        this.mailHogHttpUrl = builder.mailHogHttpUrl;
        this.mailpitHttpsUrl = builder.mailpitHttpsUrl;
        this.mailpitHttpUrl = builder.mailpitHttpUrl;
        this.services = builder.services != null ? builder.services : new HashMap<>();
        this.databaseInfo = builder.databaseInfo;
        this.primaryUrl = builder.primaryUrl;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private @Nullable String name;
        private @Nullable String phpVersion;
        private @Nullable Status status;
        private @Nullable String mailHogHttpsUrl;
        private @Nullable String mailHogHttpUrl;
        private @Nullable String mailpitHttpsUrl;
        private @Nullable String mailpitHttpUrl;
        private @Nullable Map<String, Service> services;
        private @Nullable DatabaseInfo databaseInfo;
        private @Nullable String primaryUrl;

        private Builder() {}

        public Builder name(@Nullable String name) {
            this.name = name;
            return this;
        }

        public Builder phpVersion(@Nullable String phpVersion) {
            this.phpVersion = phpVersion;
            return this;
        }

        public Builder status(@Nullable Status status) {
            this.status = status;
            return this;
        }

        public Builder mailHogHttpsUrl(@Nullable String mailHogHttpsUrl) {
            this.mailHogHttpsUrl = mailHogHttpsUrl;
            return this;
        }

        public Builder mailHogHttpUrl(@Nullable String mailHogHttpUrl) {
            this.mailHogHttpUrl = mailHogHttpUrl;
            return this;
        }

        public Builder mailpitHttpsUrl(@Nullable String mailpitHttpsUrl) {
            this.mailpitHttpsUrl = mailpitHttpsUrl;
            return this;
        }

        public Builder mailpitHttpUrl(@Nullable String mailpitHttpUrl) {
            this.mailpitHttpUrl = mailpitHttpUrl;
            return this;
        }

        public Builder services(@Nullable Map<String, Service> services) {
            this.services = services;
            return this;
        }

        public Builder databaseInfo(@Nullable DatabaseInfo databaseInfo) {
            this.databaseInfo = databaseInfo;
            return this;
        }

        public Builder primaryUrl(@Nullable String primaryUrl) {
            this.primaryUrl = primaryUrl;
            return this;
        }

        public Description build() {
            return new Description(this);
        }
    }

    public @Nullable String getName() {
        return name;
    }

    public @Nullable String getPhpVersion() {
        return this.phpVersion;
    }

    public @Nullable Status getStatus() {
        return this.status;
    }

    public @Nullable String getMailHogHttpsUrl() {
        return this.mailHogHttpsUrl;
    }

    public @Nullable String getMailHogHttpUrl() {
        return this.mailHogHttpUrl;
    }

    public @Nullable String getMailpitHttpsUrl() {
        return mailpitHttpsUrl;
    }

    public @Nullable String getMailpitHttpUrl() {
        return mailpitHttpUrl;
    }

    public @NotNull Map<String, Service> getServices() {
        if (this.services == null) {
            return new HashMap<>();
        }

        var serviceMap = new HashMap<>(this.services);

        if (this.getMailHogHttpsUrl() != null || this.getMailHogHttpUrl() != null) {
            serviceMap.put("mailhog", new Service("ddev-" + this.getName() + "-mailhog", this.getMailHogHttpsUrl(), this.getMailHogHttpUrl()));
        }

        if (this.getMailpitHttpsUrl() != null || this.getMailpitHttpUrl() != null) {
            serviceMap.put("mailpit", new Service("ddev-" + this.getName() + "-mailpit", this.getMailpitHttpsUrl(), this.getMailpitHttpsUrl()));
        }

        return serviceMap;
    }

    public @Nullable DatabaseInfo getDatabaseInfo() {
        return databaseInfo;
    }

    public @Nullable String getPrimaryUrl() {
        return primaryUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Description that = (Description) o;
        return Objects.equals(getName(), that.getName()) && Objects.equals(getPhpVersion(), that.getPhpVersion()) && getStatus() == that.getStatus() && Objects.equals(getMailHogHttpsUrl(), that.getMailHogHttpsUrl()) && Objects.equals(getMailHogHttpUrl(), that.getMailHogHttpUrl()) && Objects.equals(getMailpitHttpsUrl(), that.getMailpitHttpsUrl()) && Objects.equals(getMailpitHttpUrl(), that.getMailpitHttpUrl()) && Objects.equals(getServices(), that.getServices()) && Objects.equals(getDatabaseInfo(), that.getDatabaseInfo()) && Objects.equals(getPrimaryUrl(), that.getPrimaryUrl());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getPhpVersion(), getStatus(), getMailHogHttpsUrl(), getMailHogHttpUrl(), getMailpitHttpsUrl(), getMailpitHttpUrl(), getServices(), getDatabaseInfo(), getPrimaryUrl());
    }

    @Override
    public String toString() {
        return "Description{" +
                "name='" + name + '\'' +
                ", phpVersion='" + phpVersion + '\'' +
                ", status=" + status +
                ", mailHogHttpsUrl='" + mailHogHttpsUrl + '\'' +
                ", mailHogHttpUrl='" + mailHogHttpUrl + '\'' +
                ", mailpitHttpsUrl='" + mailpitHttpsUrl + '\'' +
                ", mailpitHttpUrl='" + mailpitHttpUrl + '\'' +
                ", services=" + services +
                ", databaseInfo=" + databaseInfo +
                ", primaryUrl='" + primaryUrl + '\'' +
                '}';
    }
}
