package de.php_perfect.intellij.ddev.version;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.StateInitializedListener;
import de.php_perfect.intellij.ddev.notification.DdevNotifier;
import de.php_perfect.intellij.ddev.state.State;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Offers to delete the Docker images of the previous DDEV version once a newer DDEV is detected,
 * however it was upgraded.
 */
public final class DdevUpgradeListener implements StateInitializedListener {
    static final @NotNull String LAST_SEEN_VERSION = "de.php_perfect.intellij.ddev.lastSeenDdevVersion";

    private final @NotNull Project project;

    public DdevUpgradeListener(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public void onStateInitialized(@NotNull State state) {
        final Version current = state.getDdevVersion();

        if (current == null) {
            return;
        }

        final String previous = recordVersion(PropertiesComponent.getInstance(), current);
        if (previous != null) {
            DdevNotifier.getInstance(this.project).notifyDdevUpgraded(previous, current.toString());
        }
    }

    /**
     * Stores {@code current} as the last seen version and returns the version it replaces when
     * that one is older, so only the first project to see an upgrade reports it.
     */
    static synchronized @Nullable String recordVersion(@NotNull PropertiesComponent properties, @NotNull Version current) {
        final String stored = properties.getValue(LAST_SEEN_VERSION);
        properties.setValue(LAST_SEEN_VERSION, current.toString());

        if (stored == null || stored.equals(current.toString())) {
            return null;
        }
        try {
            return new Version(stored).compareTo(current) < 0 ? stored : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
