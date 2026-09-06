package de.php_perfect.intellij.ddev.cmd;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A DDEV project as returned by {@code ddev list}.
 */
public final class DdevProject {
    private final @Nullable String name;

    @SerializedName("approot")
    private final @Nullable String appRoot;

    @SerializedName("shortroot")
    private final @Nullable String shortRoot;

    private final @Nullable Description.Status status;

    private final @Nullable String statusDesc;

    private final @Nullable String type;

    private final @Nullable String primaryUrl;

    private final @Nullable String docroot;

    public DdevProject(@Nullable String name, @Nullable String appRoot, @Nullable String shortRoot,
                       @Nullable Description.Status status, @Nullable String statusDesc,
                       @Nullable String type, @Nullable String primaryUrl, @Nullable String docroot) {
        this.name = name;
        this.appRoot = appRoot;
        this.shortRoot = shortRoot;
        this.status = status;
        this.statusDesc = statusDesc;
        this.type = type;
        this.primaryUrl = primaryUrl;
        this.docroot = docroot;
    }

    public @Nullable String getName() {
        return this.name;
    }

    public @Nullable String getAppRoot() {
        return this.appRoot;
    }

    public DdevProject withAppRoot(@Nullable String appRoot) {
        return new DdevProject(this.name, appRoot, this.shortRoot, this.status, this.statusDesc,
                this.type, this.primaryUrl, this.docroot);
    }

    public @Nullable String getDocroot() {
        return this.docroot;
    }

    public @Nullable String getShortRoot() {
        return this.shortRoot;
    }

    public @Nullable Description.Status getStatus() {
        return this.status;
    }

    public @Nullable String getStatusDesc() {
        return this.statusDesc;
    }

    public @Nullable String getType() {
        return this.type;
    }

    public @Nullable String getPrimaryUrl() {
        return this.primaryUrl;
    }

    public boolean isRunning() {
        return this.status == Description.Status.RUNNING;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DdevProject that = (DdevProject) o;
        return Objects.equals(this.name, that.name) && Objects.equals(this.appRoot, that.appRoot)
                && Objects.equals(this.shortRoot, that.shortRoot) && this.status == that.status
                && Objects.equals(this.statusDesc, that.statusDesc) && Objects.equals(this.type, that.type)
                && Objects.equals(this.primaryUrl, that.primaryUrl) && Objects.equals(this.docroot, that.docroot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.appRoot, this.shortRoot, this.status, this.statusDesc, this.type, this.primaryUrl, this.docroot);
    }

    @Override
    public String toString() {
        return "DdevProject{" +
                "name='" + this.name + '\'' +
                ", appRoot='" + this.appRoot + '\'' +
                ", docroot='" + this.docroot + '\'' +
                ", shortRoot='" + this.shortRoot + '\'' +
                ", status=" + this.status +
                ", statusDesc='" + this.statusDesc + '\'' +
                ", type='" + this.type + '\'' +
                ", primaryUrl='" + this.primaryUrl + '\'' +
                '}';
    }
}
