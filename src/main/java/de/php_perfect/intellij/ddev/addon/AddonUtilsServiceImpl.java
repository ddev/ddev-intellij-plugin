package de.php_perfect.intellij.ddev.addon;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.cmd.DdevAddon;
import de.php_perfect.intellij.ddev.cmd.DdevRunnerImpl;
import de.php_perfect.intellij.ddev.cmd.ProcessExecutor;
import de.php_perfect.intellij.ddev.cmd.parser.JsonUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Implementation of the AddonUtilsService interface.
 */
public class AddonUtilsServiceImpl implements AddonUtilsService {
    private static final Logger LOG = Logger.getInstance(AddonUtilsServiceImpl.class);
    private static final int COMMAND_TIMEOUT = 60000; // 60 seconds

    @Override
    @Nullable
    public Map<String, Object> executeAddonCommand(@NotNull Project project, @NotNull String... parameters) {
        try {
            // Build the command line
            GeneralCommandLine commandLine = DdevRunnerImpl.createCommandLine("add-on", project);
            for (String param : parameters) {
                commandLine.addParameter(param);
            }

            // Execute the command
            ProcessOutput output = executeCommand(commandLine);
            if (output == null) {
                return null;
            }

            // Check the exit code
            if (output.getExitCode() != 0) {
                LOG.warn("DDEV command failed: " + output.getStderr());
                return null;
            }

            // Parse the JSON output
            String stdout = output.getStdout();
            if (stdout.isEmpty()) {
                LOG.warn("Empty output from DDEV command");
                return null;
            }

            // Parse the JSON
            return JsonUtil.parseJson(stdout);
        } catch (Exception e) {
            LOG.error("Error executing DDEV addon command", e);
            return null;
        }
    }

    /**
     * Executes a command line and returns the process output.
     *
     * @param commandLine The command line to execute
     * @return The process output, or null if the command failed
     */
    @Nullable
    private ProcessOutput executeCommand(@NotNull GeneralCommandLine commandLine) {
        try {
            return ProcessExecutor.getInstance().executeCommandLine(commandLine, COMMAND_TIMEOUT, false);
        } catch (ExecutionException e) {
            LOG.error("Failed to execute DDEV command: " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    @NotNull
    public String processAddonName(@NotNull String name) {
        if (name.startsWith("ddev-")) {
            return name.substring(5);
        }
        return name;
    }

    @Override
    public boolean isOfficialAddon(@Nullable String vendorName) {
        return "ddev".equals(vendorName);
    }

    @Override
    @NotNull
    public String getAddonType(@Nullable String vendorName) {
        return isOfficialAddon(vendorName) ? "official" : "community";
    }

    /**
     * Generic method to parse a list of add-ons from a JSON object.
     *
     * @param jsonObject The JSON object to parse
     * @param parser The function to parse each add-on JSON object
     * @param errorMessage The error message to log if parsing fails
     * @return A list of add-ons, or an empty list if parsing failed
     */
    @NotNull
    private List<DdevAddon> parseAddons(
            @Nullable Map<String, Object> jsonObject,
            Function<Map<String, Object>, DdevAddon> parser,
            String errorMessage) {

        if (jsonObject == null) {
            return Collections.emptyList();
        }

        try {
            List<Map<String, Object>> rawAddons = JsonUtil.getList(jsonObject, "raw");
            List<DdevAddon> addons = new ArrayList<>();

            for (Map<String, Object> addonJson : rawAddons) {
                parseAddon(parser, addonJson, addons);
            }

            return addons;
        } catch (Exception e) {
            LOG.error(errorMessage, e);
            return Collections.emptyList();
        }
    }

    /**
     * Parses a single addon and adds it to the list if successful.
     *
     * @param parser    The function to parse the addon JSON object
     * @param addonJson The JSON object to parse
     * @param addons    The list to add the parsed addon to
     */
    private void parseAddon(
            Function<Map<String, Object>, DdevAddon> parser,
            Map<String, Object> addonJson,
            List<DdevAddon> addons) {
        try {
            DdevAddon addon = parser.apply(addonJson);
            if (addon != null) {
                addons.add(addon);
            }
        } catch (Exception e) {
            // Skip this addon if there's an issue with it
            LOG.warn("Skipping addon due to error: " + e.getMessage());
        }
    }

    @Override
    @NotNull
    public List<DdevAddon> parseAvailableAddons(@Nullable Map<String, Object> jsonObject) {
        return parseAddons(
                jsonObject,
                addonJson -> {
                    String name = JsonUtil.getString(addonJson, "name");
                    String fullName = JsonUtil.getString(addonJson, "full_name");
                    String description = JsonUtil.getString(addonJson, "description");
                    int stars = JsonUtil.getInt(addonJson, "stargazers_count");

                    // Extract owner information
                    Map<String, Object> ownerJson = JsonUtil.getMap(addonJson, "owner");
                    String vendorName = ownerJson != null ? JsonUtil.getString(ownerJson, "login") : null;

                    // Process the addon name and determine its type
                    String addonName = processAddonName(name);
                    String type = getAddonType(vendorName);

                    return new DdevAddon(
                            addonName,
                            description,
                            null,
                            type,
                            stars,
                            vendorName,
                            fullName
                    );
                },
                "Failed to parse available addons"
        );
    }

    @Override
    @NotNull
    public List<DdevAddon> parseInstalledAddons(@Nullable Map<String, Object> jsonObject) {
        return parseAddons(
                jsonObject,
                addonJson -> {
                    // Get the addon name and repository
                    String name = JsonUtil.getString(addonJson, "Name");
                    String repository = JsonUtil.getString(addonJson, "Repository");
                    String version = JsonUtil.getString(addonJson, "Version");

                    // Process the addon name
                    String addonName = processAddonName(name);

                    // Extract vendor name from repository
                    String vendorName = null;
                    String fullName = addonName;
                    if (repository.contains("/")) {
                        String[] parts = repository.split("/");
                        if (parts.length > 1) {
                            vendorName = parts[0];
                            fullName = repository;
                        }
                    }

                    // Determine the type
                    String type = getAddonType(vendorName);

                    return new DdevAddon(
                            addonName,
                            repository,
                            version,
                            type,
                            0,
                            vendorName,
                            fullName
                    );
                },
                "Failed to parse installed addons"
        );
    }
}
