package de.php_perfect.intellij.ddev.cmd;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import de.php_perfect.intellij.ddev.DdevConfigArgumentProvider;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.settings.DdevSettingsState;
import de.php_perfect.intellij.ddev.state.DdevConfigLoader;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

public final class DdevRunnerImpl implements DdevRunner {
    private static final ExtensionPointName<DdevConfigArgumentProvider> CONFIG_ARGUMENT_PROVIDER_EP = ExtensionPointName.create("de.php_perfect.intellij.ddev.ddevConfigArgumentProvider");

    @Override
    public void start(@NotNull Project project) {
        final String title = DdevIntegrationBundle.message("ddev.run.start");
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("start", project), title, () -> this.updateDescription(project));
    }

    @Override
    public void restart(@NotNull Project project) {
        final String title = DdevIntegrationBundle.message("ddev.run.restart");
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("restart", project), title, () -> this.updateDescription(project));
    }

    @Override
    public void stop(@NotNull Project project) {
        final String title = DdevIntegrationBundle.message("ddev.run.stop");
        final Runner runner = Runner.getInstance(project);
        final GeneralCommandLine commandLine = this.createCommandLine("stop", project);

        if (DdevSettingsState.getInstance(project).createSnapshotOnStop) {
            commandLine.addParameters("--snapshot");
        }

        runner.run(commandLine, title, () -> this.updateDescription(project));
    }

    @Override
    public void powerOff(@NotNull Project project) {
        final String title = DdevIntegrationBundle.message("ddev.run.powerOff");
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("poweroff", project), title, () -> this.updateDescription(project));
    }

    @Override
    public void delete(@NotNull Project project) {
        final String title = DdevIntegrationBundle.message("ddev.run.delete");
        final Runner runner = Runner.getInstance(project);
        final GeneralCommandLine commandLine = this.createCommandLine("delete", project);

        if (DdevSettingsState.getInstance(project).omitSnapshotOnDelete) {
            commandLine.addParameters("--omit-snapshot");
        }

        runner.run(commandLine, title, () -> this.updateDescription(project));
    }

    @Override
    public void share(@NotNull Project project) {
        final String title = DdevIntegrationBundle.message("ddev.run.share");
        final Runner runner = Runner.getInstance(project);
        final ShareManager shareManager = ShareManager.getInstance(project);
        runner.run(this.createCommandLine("share", project), title, shareManager::stopSharing, shareManager::setShareProcessHandler);
    }

    @Override
    public void stopShare(@NotNull Project project) {
        ShareManager.getInstance(project).stopSharing();
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

    @Override
    public void createSnapshot(@NotNull Project project) {
        this.createSnapshot(project, null);
    }

    @Override
    public void createSnapshot(@NotNull Project project, @Nullable String workingDirectory) {
        final String title = DdevIntegrationBundle.message("ddev.run.createSnapshot");
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("snapshot", project, workingDirectory), title);
    }

    @Override
    public void restoreSnapshot(@NotNull Project project, @NotNull String snapshotName) {
        this.restoreSnapshot(project, null, snapshotName);
    }

    @Override
    public void restoreSnapshot(@NotNull Project project, @Nullable String workingDirectory, @NotNull String snapshotName) {
        final String title = DdevIntegrationBundle.message("ddev.run.restoreSnapshot");
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("snapshot", project, workingDirectory).withParameters("restore", snapshotName), title, () -> this.updateDescription(project));
    }

    @Override
    public void clearSnapshots(@NotNull Project project) {
        final String title = DdevIntegrationBundle.message("ddev.run.clearSnapshots");
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("snapshot", project).withParameters("--cleanup", "--yes"), title);
    }

    @Override
    public void importDatabase(@NotNull Project project, @NotNull String filePath) {
        this.importDatabase(project, null, filePath);
    }

    @Override
    public void importDatabase(@NotNull Project project, @Nullable String workingDirectory, @NotNull String filePath) {
        final String title = DdevIntegrationBundle.message("ddev.run.importDatabase");
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("import-db", project, workingDirectory).withParameters("--file=" + filePath), title);
    }

    @Override
    public void exportDatabase(@NotNull Project project, @NotNull String filePath) {
        this.exportDatabase(project, null, filePath);
    }

    @Override
    public void exportDatabase(@NotNull Project project, @Nullable String workingDirectory, @NotNull String filePath) {
        final String title = DdevIntegrationBundle.message("ddev.run.exportDatabase");
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("export-db", project, workingDirectory).withParameters("--file=" + filePath), title);
    }

    @Override
    public void enableXdebug(@NotNull Project project) {
        final String title = DdevIntegrationBundle.message("ddev.run.enableXdebug");
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("xdebug", project).withParameters("on"), title);
    }

    @Override
    public void disableXdebug(@NotNull Project project) {
        final String title = DdevIntegrationBundle.message("ddev.run.disableXdebug");
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("xdebug", project).withParameters("off"), title);
    }

    @Override
    public void mutagenReset(@NotNull Project project) {
        final String title = DdevIntegrationBundle.message("ddev.run.mutagenReset");
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("mutagen", project).withParameters("reset"), title, () -> this.updateDescription(project));
    }

    @Override
    public void deleteImages(@NotNull Project project) {
        final String title = DdevIntegrationBundle.message("ddev.run.deleteImages");
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("delete", project).withParameters("images", "--yes"), title);
    }

    @Override
    public void installAddOn(@NotNull Project project, @NotNull String addOnName) {
        final String title = DdevIntegrationBundle.message("ddev.run.installAddOn", addOnName);
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("add-on", project).withParameters("get", addOnName), title, () -> this.updateDescription(project));
    }

    @Override
    public void removeAddOn(@NotNull Project project, @NotNull String addOnName) {
        final String title = DdevIntegrationBundle.message("ddev.run.removeAddOn", addOnName);
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("add-on", project).withParameters("remove", addOnName), title, () -> this.updateDescription(project));
    }

    @Override
    public void startProject(@NotNull Project project, @NotNull String projectName, @Nullable Runnable afterCompletion) {
        final String title = DdevIntegrationBundle.message("ddev.run.startProject", projectName);
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("start", project).withParameters(projectName), title, () -> this.runAfterProjectCommand(project, afterCompletion));
    }

    @Override
    public void stopProject(@NotNull Project project, @NotNull String projectName, @Nullable Runnable afterCompletion) {
        final String title = DdevIntegrationBundle.message("ddev.run.stopProject", projectName);
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("stop", project).withParameters(projectName), title, () -> this.runAfterProjectCommand(project, afterCompletion));
    }

    @Override
    public void restartProject(@NotNull Project project, @NotNull String projectName, @Nullable Runnable afterCompletion) {
        final String title = DdevIntegrationBundle.message("ddev.run.restartProject", projectName);
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("restart", project).withParameters(projectName), title, () -> this.runAfterProjectCommand(project, afterCompletion));
    }

    @Override
    public void stopProjects(@NotNull Project project, @NotNull List<String> projectNames, @Nullable Runnable afterCompletion) {
        if (projectNames.isEmpty()) {
            return;
        }

        final String title = DdevIntegrationBundle.message("ddev.run.stopProjects", String.join(", ", projectNames));
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("stop", project).withParameters(projectNames), title, () -> this.runAfterProjectCommand(project, afterCompletion));
    }

    @Override
    public void deleteProject(@NotNull Project project, @NotNull String projectName, @Nullable Runnable afterCompletion) {
        final String title = DdevIntegrationBundle.message("ddev.run.deleteProject", projectName);
        final Runner runner = Runner.getInstance(project);
        final GeneralCommandLine commandLine = this.createCommandLine("delete", project).withParameters(projectName, "--yes");

        if (DdevSettingsState.getInstance(project).omitSnapshotOnDelete) {
            commandLine.addParameters("--omit-snapshot");
        }

        runner.run(commandLine, title, () -> this.runAfterProjectCommand(project, afterCompletion));
    }

    @Override
    public void renameProject(@NotNull Project project, @Nullable String workingDirectory, @NotNull String newName, @Nullable Runnable afterCompletion) {
        final String title = DdevIntegrationBundle.message("ddev.run.renameProject", newName);
        final Runner runner = Runner.getInstance(project);
        final GeneralCommandLine commandLine = this.createCommandLine("config", project, workingDirectory).withParameters("--project-name=" + newName);
        runner.run(commandLine, title, () -> {
            this.updateConfiguration(project);

            if (afterCompletion != null) {
                afterCompletion.run();
            }
        });
    }

    @Override
    public void createProject(@NotNull Project project, @NotNull String workingDirectory, @Nullable Runnable afterCompletion, @NotNull String... configArguments) {
        final String title = DdevIntegrationBundle.message("ddev.run.config");
        final Runner runner = Runner.getInstance(project);
        final GeneralCommandLine commandLine = this.createCommandLine("config", project, workingDirectory).withParameters(configArguments);
        runner.run(commandLine, title, afterCompletion);
    }

    @Override
    public void updateConfig(@NotNull Project project, @NotNull String... configArguments) {
        final String title = DdevIntegrationBundle.message("ddev.run.config");
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("config", project).withParameters(configArguments), title, () -> this.updateConfiguration(project));
    }

    private void runAfterProjectCommand(@NotNull Project project, @Nullable Runnable afterCompletion) {
        this.updateDescription(project);

        if (afterCompletion != null) {
            afterCompletion.run();
        }
    }

    private void openConfig(@NotNull Project project) {
        VirtualFile ddevConfig = DdevConfigLoader.getInstance(project).load();

        if (ddevConfig != null && ddevConfig.exists()) {
            FileEditorManager.getInstance(project).openFile(ddevConfig, true);
        }
    }

    private void updateDescription(Project project) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> DdevStateManager.getInstance(project).updateDescription());
    }

    private void updateConfiguration(Project project) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> DdevStateManager.getInstance(project).updateConfiguration());
    }

    private @NotNull GeneralCommandLine buildConfigCommandLine(@NotNull Project project) {
        final GeneralCommandLine commandLine = this.createCommandLine("config", project)
                .withParameters("--auto");

        for (final DdevConfigArgumentProvider ddevConfigArgumentProvider : CONFIG_ARGUMENT_PROVIDER_EP.getExtensionList()) {
            commandLine.addParameters(ddevConfigArgumentProvider.getAdditionalArguments(project));
        }

        return commandLine;
    }

    private @NotNull GeneralCommandLine createCommandLine(@NotNull String ddevAction, @NotNull Project project) {
        return this.createCommandLine(ddevAction, project, null);
    }

    private @NotNull GeneralCommandLine createCommandLine(@NotNull String ddevAction, @NotNull Project project, @Nullable String workingDirectory) {
        State state = DdevStateManager.getInstance(project).getState();

        return new PtyCommandLine(List.of(Objects.requireNonNull(state.getDdevBinary()), ddevAction))
                .withInitialRows(30)
                .withInitialColumns(120)
                .withWorkDirectory(workingDirectory != null ? workingDirectory : project.getBasePath())
                .withCharset(StandardCharsets.UTF_8)
                .withEnvironment("DDEV_NONINTERACTIVE", "true");
    }
}
