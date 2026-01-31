package de.php_perfect.intellij.ddev.addon;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.cmd.DdevAddon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Service interface for DDEV addon-related operations.
 */
public interface AddonUtilsService {
    /**
     * Executes a DDEV addon command and returns the JSON output.
     *
     * @param project    The project to execute the command for
     * @param parameters The command parameters
     * @return The parsed JSON object, or null if the command failed
     */
    @Nullable
    Map<String, Object> executeAddonCommand(@NotNull Project project, @NotNull String... parameters);

    /**
     * Processes an addon name to remove the "ddev-" prefix if present.
     *
     * @param name The addon name
     * @return The processed addon name
     */
    @NotNull
    String processAddonName(@NotNull String name);

    /**
     * Determines if an addon is official based on the vendor name.
     *
     * @param vendorName The vendor name
     * @return true if the addon is official, false otherwise
     */
    boolean isOfficialAddon(@Nullable String vendorName);

    /**
     * Gets the addon type based on whether it's official or not.
     *
     * @param vendorName The vendor name
     * @return "official" if the addon is official, "community" otherwise
     */
    @NotNull
    String getAddonType(@Nullable String vendorName);

    /**
     * Parses available addons from a JSON object.
     *
     * @param jsonObject The JSON object to parse
     * @return A list of available addons, or an empty list if parsing failed
     */
    @NotNull
    List<DdevAddon> parseAvailableAddons(@Nullable Map<String, Object> jsonObject);

    /**
     * Parses installed addons from a JSON object.
     *
     * @param jsonObject The JSON object to parse
     * @return A list of installed addons, or an empty list if parsing failed
     */
    @NotNull
    List<DdevAddon> parseInstalledAddons(@Nullable Map<String, Object> jsonObject);

    /**
     * Gets the AddonUtilsService instance.
     *
     * @return The AddonUtilsService instance
     */
    static AddonUtilsService getInstance() {
        return ApplicationManager.getApplication().getService(AddonUtilsService.class);
    }
}
