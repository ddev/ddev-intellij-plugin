package de.php_perfect.intellij.ddev.cmd;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * An add-on installed in the current project, as returned by {@code ddev add-on list --installed}.
 * DDEV serializes these manifests with capitalized JSON keys.
 */
public final class InstalledAddOn {
    @SerializedName("Name")
    private final @Nullable String name;

    @SerializedName("Repository")
    private final @Nullable String repository;

    @SerializedName("Version")
    private final @Nullable String version;

    public InstalledAddOn(@Nullable String name, @Nullable String repository, @Nullable String version) {
        this.name = name;
        this.repository = repository;
        this.version = version;
    }

    /**
     * The identifier used to remove the add-on, e.g. {@code redis}.
     */
    public @Nullable String getName() {
        return this.name;
    }

    public @Nullable String getRepository() {
        return this.repository;
    }

    public @Nullable String getVersion() {
        return this.version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InstalledAddOn that = (InstalledAddOn) o;
        return Objects.equals(this.name, that.name) && Objects.equals(this.repository, that.repository) && Objects.equals(this.version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.repository, this.version);
    }

    @Override
    public String toString() {
        return "InstalledAddOn{" +
                "name='" + this.name + '\'' +
                ", repository='" + this.repository + '\'' +
                ", version='" + this.version + '\'' +
                '}';
    }
}
