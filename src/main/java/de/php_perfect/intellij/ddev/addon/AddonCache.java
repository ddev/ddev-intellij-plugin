package de.php_perfect.intellij.ddev.addon;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import de.php_perfect.intellij.ddev.cmd.DdevAddon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cache for DDEV addons to avoid repeated calls to the DDEV CLI.
 */
public class AddonCache {
    private static final Logger LOG = Logger.getInstance(AddonCache.class);
    private static final ConcurrentHashMap<Project, AddonCache> INSTANCES = new ConcurrentHashMap<>();
    // Cache expiration time: 24 hours in milliseconds
    private static final long CACHE_EXPIRATION_TIME = 24L * 60 * 60 * 1000;

    private final Project project;
    private final AtomicBoolean isRefreshing = new AtomicBoolean(false);
    private List<DdevAddon> availableAddons = Collections.synchronizedList(Collections.emptyList());
    private final AtomicLong lastRefreshTime = new AtomicLong(0);

    /**
     * Gets the AddonCache instance for the given project.
     *
     * @param project The project to get the cache for
     * @return The AddonCache instance
     */
    public static @NotNull AddonCache getInstance(@NotNull Project project) {
        return INSTANCES.computeIfAbsent(project, AddonCache::new);
    }

    private AddonCache(@NotNull Project project) {
        this.project = project;

        // Initialize the cache in the background
        refreshCacheAsync();

        // Register a project dispose listener to remove the cache when the project is closed
        ProjectManager.getInstance().addProjectManagerListener(project, new ProjectManagerListener() {
            @Override
            public void projectClosed(@NotNull Project closedProject) {
                if (project.equals(closedProject)) {
                    INSTANCES.remove(project);
                }
            }
        });
    }

    /**
     * Gets the list of available addons from the cache.
     * If the cache is empty or expired, it will be refreshed asynchronously.
     *
     * @return The list of available addons
     */
    public @NotNull List<DdevAddon> getAvailableAddons() {
        // If the cache is empty or expired, refresh it asynchronously
        if (availableAddons.isEmpty() || isCacheExpired()) {
            refreshCacheAsync();
        }
        return availableAddons;
    }

    /**
     * Checks if the cache is expired.
     *
     * @return true if the cache is expired, false otherwise
     */
    private boolean isCacheExpired() {
        return System.currentTimeMillis() - lastRefreshTime.get() > CACHE_EXPIRATION_TIME;
    }

    /**
     * Refreshes the cache asynchronously.
     */
    public void refreshCacheAsync() {
        // If already refreshing, don't start another refresh
        if (isRefreshing.compareAndSet(false, true)) {
            LOG.debug("Starting async refresh of addon cache for project: " + project.getName());

            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    List<DdevAddon> addons = fetchAvailableAddons();
                    if (addons != null) {
                        availableAddons = addons;
                        lastRefreshTime.set(System.currentTimeMillis());
                        LOG.info("Refreshed addon cache for project: " + project.getName() + ", found " + addons.size() + " addons");
                    }
                } catch (Exception e) {
                    LOG.error("Failed to refresh addon cache for project: " + project.getName(), e);
                } finally {
                    isRefreshing.set(false);
                }
            });
        }
    }

    /**
     * Fetches the list of available addons from the DDEV CLI.
     *
     * @return The list of available addons, or null if the operation failed
     */
    @Nullable
    private List<DdevAddon> fetchAvailableAddons() {
        // Execute the DDEV command to get all available add-ons
        AddonUtilsService addonUtilsService = AddonUtilsService.getInstance();
        Map<String, Object> jsonObject = addonUtilsService.executeAddonCommand(project, "list", "--all", "--json-output");

        // Parse the JSON response into a list of DdevAddon objects
        List<DdevAddon> addons = addonUtilsService.parseAvailableAddons(jsonObject);

        // Return null if no addons were found (to trigger appropriate logging)
        return addons.isEmpty() ? null : addons;
    }
}
