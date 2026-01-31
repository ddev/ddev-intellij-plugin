package de.php_perfect.intellij.ddev.cmd;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import de.php_perfect.intellij.ddev.DdevConfigArgumentProvider;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.addon.AddonCache;
import de.php_perfect.intellij.ddev.addon.AddonUtilsService;
import de.php_perfect.intellij.ddev.state.DdevConfigLoader;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DdevRunnerImpl implements DdevRunner {
    private static final Logger LOG = Logger.getInstance(DdevRunnerImpl.class);
    private static final ExtensionPointName<DdevConfigArgumentProvider> CONFIG_ARGUMENT_PROVIDER_EP = ExtensionPointName.create("de.php_perfect.intellij.ddev.ddevConfigArgumentProvider");

    @Override
    public void start(@NotNull Project project) {
        final String title = DdevIntegrationBundle.message("ddev.run.start");
        final Runner runner = Runner.getInstance(project);
        runner.run(DdevRunnerImpl.createCommandLine("start", project), title, () -> this.updateDescription(project));
    }

    @Override
    public void restart(@NotNull Project project) {
        final String title = DdevIntegrationBundle.message("ddev.run.restart");
        final Runner runner = Runner.getInstance(project);
        runner.run(DdevRunnerImpl.createCommandLine("restart", project), title, () -> this.updateDescription(project));
    }

    @Override
    public void stop(@NotNull Project project) {
        final String title = DdevIntegrationBundle.message("ddev.run.stop");
        final Runner runner = Runner.getInstance(project);
        runner.run(DdevRunnerImpl.createCommandLine("stop", project), title, () -> this.updateDescription(project));
    }

    @Override
    public void powerOff(@NotNull Project project) {
        final String title = DdevIntegrationBundle.message("ddev.run.powerOff");
        final Runner runner = Runner.getInstance(project);
        runner.run(DdevRunnerImpl.createCommandLine("poweroff", project), title, () -> this.updateDescription(project));
    }

    @Override
    public void delete(@NotNull Project project) {
        final String title = DdevIntegrationBundle.message("ddev.run.delete");
        final Runner runner = Runner.getInstance(project);
        runner.run(DdevRunnerImpl.createCommandLine("delete", project), title, () -> this.updateDescription(project));
    }

    @Override
    public void share(@NotNull Project project) {
        final String title = DdevIntegrationBundle.message("ddev.run.share");
        final Runner runner = Runner.getInstance(project);
        runner.run(DdevRunnerImpl.createCommandLine("share", project), title, () -> this.updateDescription(project));
    }

    @Override
    public void config(@NotNull Project project) {
        final String title = DdevIntegrationBundle.message("ddev.run.config");
        final Runner runner = Runner.getInstance(project);
        runner.run(this.buildConfigCommandLine(project), title, () -> {
            this.updateConfiguration(project);
            this.openConfig(project);
        });
    }

    private void openConfig(@NotNull Project project) {
        VirtualFile ddevConfig = DdevConfigLoader.getInstance(project).load();

        if (ddevConfig != null && ddevConfig.exists()) {
            FileEditorManager.getInstance(project).openFile(ddevConfig, true);
        }
    }

    private void updateDescription(Project project) {
        ApplicationManager.getApplication().executeOnPooledThread(() ->
            DdevStateManager.getInstance(project).updateDescription()
            // No need to refresh the addon cache here, it will be refreshed when needed
        );
    }

    private void updateConfiguration(Project project) {
        ApplicationManager.getApplication().executeOnPooledThread(() ->
            DdevStateManager.getInstance(project).updateConfiguration()
            // No need to refresh the addon cache here, it will be refreshed when needed
        );
    }

    private @NotNull GeneralCommandLine buildConfigCommandLine(@NotNull Project project) {
        final GeneralCommandLine commandLine = DdevRunnerImpl.createCommandLine("config", project)
                .withParameters("--auto");

        for (final DdevConfigArgumentProvider ddevConfigArgumentProvider : CONFIG_ARGUMENT_PROVIDER_EP.getExtensionList()) {
            commandLine.addParameters(ddevConfigArgumentProvider.getAdditionalArguments(project));
        }

        return commandLine;
    }

    public static @NotNull GeneralCommandLine createCommandLine(@NotNull String ddevAction, @NotNull Project project) {
        State state = DdevStateManager.getInstance(project).getState();

        return new PtyCommandLine(List.of(Objects.requireNonNull(state.getDdevBinary()), ddevAction))
                .withInitialRows(30)
                .withInitialColumns(120)
                .withWorkDirectory(project.getBasePath())
                .withCharset(StandardCharsets.UTF_8)
                .withEnvironment("DDEV_NONINTERACTIVE", "true");
    }

    @Override
    public @NotNull List<DdevAddon> getAvailableAddons(@NotNull Project project) {
        LOG.debug("Getting available add-ons for project: " + project.getName());

        State state = DdevStateManager.getInstance(project).getState();

        if (!state.isAvailable() || !state.isConfigured()) {
            LOG.warn("DDEV is not available or not configured for project: " + project.getName());
            return new ArrayList<>();
        }

        // Get the list of installed add-ons to exclude them from the available add-ons
        List<String> installedAddonNames = getInstalledAddons(project).stream()
                .map(DdevAddon::getName)
                .toList();

        // Get the available addons from the cache
        // The getAvailableAddons method will handle refreshing if needed
        List<DdevAddon> allAddons = AddonCache.getInstance(project).getAvailableAddons();

        // Filter out installed add-ons
        return allAddons.stream()
                .filter(addon -> !installedAddonNames.contains(addon.getName()))
                .toList();
    }



    @Override
    public @NotNull List<DdevAddon> getInstalledAddons(@NotNull Project project) {
        LOG.debug("Getting installed add-ons for project: " + project.getName());
        State state = DdevStateManager.getInstance(project).getState();

        if (!state.isAvailable() || !state.isConfigured()) {
            LOG.warn("DDEV is not available or not configured for project: " + project.getName());
            return Collections.emptyList();
        }

        // Execute the DDEV command to get installed add-ons
        AddonUtilsService addonUtilsService = AddonUtilsService.getInstance();
        Map<String, Object> jsonObject = addonUtilsService.executeAddonCommand(project, "list", "--installed", "--json-output");

        // Parse the JSON response into a list of DdevAddon objects
        return addonUtilsService.parseInstalledAddons(jsonObject);
    }

    @Override
    public void addAddon(@NotNull Project project, @NotNull DdevAddon addon) {
        String installName = addon.getInstallName();
        LOG.debug("Adding add-on: " + installName + " to project: " + project.getName());
        final String title = DdevIntegrationBundle.message("ddev.run.addAddon", addon.getName());
        final Runner runner = Runner.getInstance(project);

        // Use the DDEV add-on get command to properly add the add-on
        GeneralCommandLine commandLine = DdevRunnerImpl.createCommandLine("add-on", project)
                .withParameters("get", installName);

        runner.run(commandLine, title, () -> this.updateDescription(project));
    }

    @Override
    public void deleteAddon(@NotNull Project project, @NotNull DdevAddon addon) {
        // For the installed add-ons list, we need to use the original name from the list
        // This is the name without any modifications
        String addonName = addon.getName();

        LOG.debug("Removing add-on: " + addonName + " from project: " + project.getName());
        final String title = DdevIntegrationBundle.message("ddev.run.deleteAddon", addon.getName());
        final Runner runner = Runner.getInstance(project);

        // Use the DDEV add-on remove command to properly remove the add-on
        GeneralCommandLine commandLine = DdevRunnerImpl.createCommandLine("add-on", project)
                .withParameters("remove", addonName);

        runner.run(commandLine, title, () -> this.updateDescription(project));
    }
}
