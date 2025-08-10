package de.php_perfect.intellij.ddev.cmd;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a DDEV add-on that can be added to or removed from a DDEV project.
 */
public class DdevAddon {
    private final @NotNull String name;
    private final @NotNull String description;
    private final @Nullable String version;
    private final @Nullable String type;
    private final int stars;
    private final @Nullable String vendorName;
    private final @Nullable String fullName;

    public DdevAddon(@NotNull String name, @NotNull String description, @Nullable String version,
                      @Nullable String type, int stars, @Nullable String vendorName, @Nullable String fullName) {
        this.name = name;
        this.description = description;
        this.version = version;
        this.type = type;
        this.stars = stars;
        this.vendorName = vendorName;
        this.fullName = fullName;
    }

    /**
     * Gets the name of the add-on.
     *
     * @return The add-on name
     */
    public @NotNull String getName() {
        return name;
    }

    /**
     * Gets the description of the add-on.
     *
     * @return The add-on description
     */
    public @NotNull String getDescription() {
        return description;
    }

    /**
     * Gets the version of the add-on, if available.
     *
     * @return The add-on version, or null if not specified
     */
    public @Nullable String getVersion() {
        return version;
    }

    /**
     * Gets the type of the add-on, if available.
     *
     * @return The add-on type, or null if not specified
     */
    public @Nullable String getType() {
        return type;
    }

    /**
     * Gets the number of stars of the add-on.
     *
     * @return The add-on stars
     */
    public int getStars() {
        return stars;
    }

    /**
     * Gets the vendor name of the add-on, if available.
     *
     * @return The add-on vendor name, or null if not specified
     */
    public @Nullable String getVendorName() {
        return vendorName;
    }

    /**
     * Gets the full name of the add-on (with vendor prefix), if available.
     * This is the name that should be used for installation.
     *
     * @return The full add-on name, or null if not specified
     */
    public @Nullable String getFullName() {
        return fullName;
    }

    /**
     * Gets the name to use for installation.
     * If fullName is available, it will be used; otherwise, name will be used.
     *
     * @return The name to use for installation
     */
    public @NotNull String getInstallName() {
        if (fullName != null && !fullName.isEmpty()) {
            return fullName;
        }
        return name;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // Add name
        sb.append(name);

        // Add vendor name as a comment if available
        if (vendorName != null && !vendorName.isEmpty()) {
            sb.append(" (").append(vendorName).append(")");
        }

        // Add version if available
        if (version != null) {
            sb.append(" v").append(version);
        }

        // Add type if available
        if (type != null) {
            sb.append(" [").append(type).append("]");
        }

        // Add stars if available
        if (stars > 0) {
            sb.append(" ★").append(stars);
        }

        return sb.toString();
    }
}
