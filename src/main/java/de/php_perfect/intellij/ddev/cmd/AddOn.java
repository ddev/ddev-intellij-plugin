package de.php_perfect.intellij.ddev.cmd;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * An add-on available in the DDEV add-on registry, as returned by {@code ddev add-on list --all}.
 */
public final class AddOn {
    private final @Nullable String title;

    private final @Nullable String description;

    private final @Nullable String type;

    private final @Nullable String tagName;

    public AddOn(@Nullable String title, @Nullable String description, @Nullable String type, @Nullable String tagName) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.tagName = tagName;
    }

    /**
     * The identifier used to install the add-on, e.g. {@code ddev/ddev-redis}.
     */
    public @Nullable String getTitle() {
        return this.title;
    }

    public @Nullable String getDescription() {
        return this.description;
    }

    /**
     * Either {@code official} or {@code contrib}.
     */
    public @Nullable String getType() {
        return this.type;
    }

    public @Nullable String getTagName() {
        return this.tagName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddOn addOn = (AddOn) o;
        return Objects.equals(this.title, addOn.title) && Objects.equals(this.description, addOn.description) && Objects.equals(this.type, addOn.type) && Objects.equals(this.tagName, addOn.tagName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.title, this.description, this.type, this.tagName);
    }

    @Override
    public String toString() {
        return "AddOn{" +
                "title='" + this.title + '\'' +
                ", description='" + this.description + '\'' +
                ", type='" + this.type + '\'' +
                ", tagName='" + this.tagName + '\'' +
                '}';
    }
}
