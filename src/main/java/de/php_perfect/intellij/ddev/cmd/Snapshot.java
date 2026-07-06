package de.php_perfect.intellij.ddev.cmd;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A database snapshot as returned by {@code ddev snapshot --list}.
 * DDEV serializes these with capitalized JSON keys.
 */
public final class Snapshot {
    @SerializedName("Name")
    private final @Nullable String name;

    @SerializedName("Created")
    private final @Nullable String created;

    public Snapshot(@Nullable String name, @Nullable String created) {
        this.name = name;
        this.created = created;
    }

    public @Nullable String getName() {
        return this.name;
    }

    public @Nullable String getCreated() {
        return this.created;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Snapshot snapshot = (Snapshot) o;
        return Objects.equals(this.name, snapshot.name) && Objects.equals(this.created, snapshot.created);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.created);
    }

    @Override
    public String toString() {
        return "Snapshot{" +
                "name='" + this.name + '\'' +
                ", created='" + this.created + '\'' +
                '}';
    }
}
