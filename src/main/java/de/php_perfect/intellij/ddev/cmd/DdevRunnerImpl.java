package de.php_perfect.intellij.ddev.cmd;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import de.php_perfect.intellij.ddev.DdevConfigArgumentProvider;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.wsl.WslAware;
import de.php_perfect.intellij.ddev.settings.DdevSettingsState;
import de.php_perfect.intellij.ddev.state.DdevConfigLoader;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import de.php_perfect.intellij.ddev.wordpress.WordPressImportReconciler;
import de.php_perfect.intellij.ddev.wordpress.WordPressConfigManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public final class DdevRunnerImpl implements DdevRunner {
    private static final Logger LOG = Logger.getInstance(DdevRunnerImpl.class);
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
        final GeneralCommandLine commandLine = this.createCommandLine("delete", project).withParameters("--yes");

        final DdevSettingsState settings = DdevSettingsState.getInstance(project);
        if (settings.omitSnapshotOnDelete || settings.deleteDdevFolderOnDelete) {
            commandLine.addParameters("--omit-snapshot");
        }

        runner.runOnSuccess(commandLine, title, () -> {
            this.deleteDdevFolderIfConfigured(project, project.getBasePath());
            this.updateDescription(project);
        });
    }

    @Override
    public void share(@NotNull Project project) {
        this.share(project, null, WordPressConfigManager.docroot(project));
    }

    @Override
    public void share(@NotNull Project project, @Nullable String workingDirectory, @Nullable String docroot) {
        final String title = DdevIntegrationBundle.message("ddev.run.share");
        final Runner runner = Runner.getInstance(project);
        final ShareManager shareManager = ShareManager.getInstance(project);
        runner.run(this.createCommandLine("share", project, workingDirectory), title, shareManager::stopSharing,
                processHandler -> shareManager.setShareProcessHandler(processHandler,
                        workingDirectory != null ? workingDirectory : project.getBasePath(), docroot));
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
        this.clearSnapshots(project, null, null);
    }

    @Override
    public void clearSnapshots(@NotNull Project project, @Nullable String workingDirectory, @Nullable Runnable afterCompletion) {
        final String title = DdevIntegrationBundle.message("ddev.run.clearSnapshots");
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("snapshot", project, workingDirectory).withParameters("--cleanup", "--yes"),
                title, afterCompletion);
    }

    @Override
    public void importDatabase(@NotNull Project project, @NotNull String filePath) {
        final String workingDirectory = project.getBasePath();
        final Description description = DdevStateManager.getInstance(project).getState().getDescription();
        final String projectName = description != null ? description.getName() : null;
        final String projectType = workingDirectory != null ? DdevProjectTypeDetector.detect(workingDirectory) : null;

        if (workingDirectory != null) {
            this.importDatabase(project, workingDirectory, filePath, projectName, projectType);
        }
    }

    @Override
    public void importDatabase(@NotNull Project project, @Nullable String workingDirectory, @NotNull String filePath) {
        if (workingDirectory == null) {
            this.importDatabase(project, filePath);
            return;
        }

        this.importDatabase(project, workingDirectory, filePath, null,
                DdevProjectTypeDetector.detect(workingDirectory));
    }

    @Override
    public void importDatabase(@NotNull Project project, @NotNull String workingDirectory,
                               @NotNull String filePath, @Nullable String projectName,
                               @Nullable String projectType) {
        final String title = DdevIntegrationBundle.message("ddev.run.importDatabase");
        final Runner runner = Runner.getInstance(project);
        runner.runOnSuccess(this.createCommandLine("import-db", project, workingDirectory)
                .withParameters("--file=" + WslAware.toCommandPath(filePath, workingDirectory)), title, () -> {
            this.runAfterTargetCommand(project, workingDirectory, null);

            if ("wordpress".equals(projectType) && projectName != null) {
                WordPressImportReconciler.reconcile(project, workingDirectory, projectName);
            }
        });
    }

    @Override
    public void exportDatabase(@NotNull Project project, @NotNull String filePath) {
        this.exportDatabase(project, null, filePath);
    }

    @Override
    public void exportDatabase(@NotNull Project project, @Nullable String workingDirectory, @NotNull String filePath) {
        final String title = DdevIntegrationBundle.message("ddev.run.exportDatabase");
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("export-db", project, workingDirectory).withParameters("--file="
                + WslAware.toCommandPath(filePath, workingDirectory != null ? workingDirectory : project.getBasePath())), title);
    }

    @Override
    public void enableXdebug(@NotNull Project project) {
        this.enableXdebug(project, null, null);
    }

    @Override
    public void enableXdebug(@NotNull Project project, @Nullable String workingDirectory, @Nullable Runnable afterCompletion) {
        final String title = DdevIntegrationBundle.message("ddev.run.enableXdebug");
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("xdebug", project, workingDirectory).withParameters("on"), title,
                afterCompletion);
    }

    @Override
    public void disableXdebug(@NotNull Project project) {
        this.disableXdebug(project, null, null);
    }

    @Override
    public void disableXdebug(@NotNull Project project, @Nullable String workingDirectory, @Nullable Runnable afterCompletion) {
        final String title = DdevIntegrationBundle.message("ddev.run.disableXdebug");
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("xdebug", project, workingDirectory).withParameters("off"), title,
                afterCompletion);
    }

    @Override
    public void mutagenReset(@NotNull Project project) {
        this.mutagenReset(project, null, null);
    }

    @Override
    public void mutagenReset(@NotNull Project project, @Nullable String workingDirectory, @Nullable Runnable afterCompletion) {
        final String title = DdevIntegrationBundle.message("ddev.run.mutagenReset");
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("mutagen", project, workingDirectory).withParameters("reset"), title,
                () -> this.runAfterTargetCommand(project, workingDirectory, afterCompletion));
    }

    @Override
    public void deleteImages(@NotNull Project project) {
        final String title = DdevIntegrationBundle.message("ddev.run.deleteImages");
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("delete", project).withParameters("images", "--all", "--yes"), title);
    }

    @Override
    public void installAddOn(@NotNull Project project, @NotNull String addOnName) {
        this.installAddOn(project, null, addOnName, null);
    }

    @Override
    public void installAddOn(@NotNull Project project, @Nullable String workingDirectory, @NotNull String addOnName,
                             @Nullable Runnable afterCompletion) {
        final String title = DdevIntegrationBundle.message("ddev.run.installAddOn", addOnName);
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("add-on", project, workingDirectory).withParameters("get", addOnName), title,
                () -> this.runAfterTargetCommand(project, workingDirectory, afterCompletion));
    }

    @Override
    public void removeAddOn(@NotNull Project project, @NotNull String addOnName) {
        this.removeAddOn(project, null, addOnName, null);
    }

    @Override
    public void removeAddOn(@NotNull Project project, @Nullable String workingDirectory, @NotNull String addOnName,
                            @Nullable Runnable afterCompletion) {
        final String title = DdevIntegrationBundle.message("ddev.run.removeAddOn", addOnName);
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("add-on", project, workingDirectory).withParameters("remove", addOnName), title,
                () -> this.runAfterTargetCommand(project, workingDirectory, afterCompletion));
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
        final GeneralCommandLine commandLine = this.createCommandLine("stop", project).withParameters(projectName);

        if (DdevSettingsState.getInstance(project).createSnapshotOnStop) {
            commandLine.addParameters("--snapshot");
        }

        runner.run(commandLine, title, () -> this.runAfterProjectCommand(project, afterCompletion));
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
        final GeneralCommandLine commandLine = this.createCommandLine("stop", project).withParameters(projectNames);

        if (DdevSettingsState.getInstance(project).createSnapshotOnStop) {
            commandLine.addParameters("--snapshot");
        }

        runner.run(commandLine, title, () -> this.runAfterProjectCommand(project, afterCompletion));
    }

    @Override
    public void deleteProject(@NotNull Project project, @NotNull String projectName, @Nullable Runnable afterCompletion) {
        this.deleteProject(project, projectName, null, afterCompletion);
    }

    @Override
    public void deleteProject(@NotNull Project project, @NotNull String projectName, @Nullable String workingDirectory,
                              @Nullable Runnable afterCompletion) {
        final String title = DdevIntegrationBundle.message("ddev.run.deleteProject", projectName);
        final Runner runner = Runner.getInstance(project);
        final GeneralCommandLine commandLine = this.createCommandLine("delete", project).withParameters(projectName, "--yes");

        final DdevSettingsState settings = DdevSettingsState.getInstance(project);
        if (settings.omitSnapshotOnDelete || settings.deleteDdevFolderOnDelete) {
            commandLine.addParameters("--omit-snapshot");
        }

        runner.runOnSuccess(commandLine, title, () -> {
            this.deleteDdevFolderIfConfigured(project, workingDirectory);
            this.runAfterProjectCommand(project, afterCompletion);
        });
    }

    @Override
    public void renameProject(@NotNull Project project, @Nullable String workingDirectory,
                              @NotNull String currentName, @NotNull String newName,
                              @Nullable Runnable afterCompletion) {
        final String binary = DdevStateManager.getInstance(project).getState().getDdevBinary();
        if (binary == null) {
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final boolean hasDatabase = Ddev.getInstance().describeProject(binary, project, currentName)
                        .getDatabaseInfo() != null;
                ApplicationManager.getApplication().invokeLater(() -> this.runRenameProject(project,
                        workingDirectory, currentName, newName, hasDatabase, afterCompletion));
            } catch (CommandFailedException exception) {
                ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog(project,
                        DdevIntegrationBundle.message("renameProject.inspectFailed", currentName),
                        DdevIntegrationBundle.message("renameProject.title")));
            }
        });
    }

    private void runRenameProject(@NotNull Project project, @Nullable String workingDirectory,
                                  @NotNull String currentName, @NotNull String newName, boolean hasDatabase,
                                  @Nullable Runnable afterCompletion) {
        final Runner runner = Runner.getInstance(project);
        final String snapshotName = "rename__" + System.currentTimeMillis();
        final Runnable stopOldProject = () -> runner.runOnSuccess(
                this.createCommandLine("stop", project).withParameters("--unlist", currentName),
                DdevIntegrationBundle.message("ddev.run.renameProject.stop", currentName),
                () -> runner.runOnSuccess(this.createCommandLine("config", project, workingDirectory)
                                .withParameters("--project-name=" + newName),
                        DdevIntegrationBundle.message("ddev.run.renameProject", newName),
                        () -> runner.runOnSuccess(this.createCommandLine("start", project, workingDirectory),
                                DdevIntegrationBundle.message("ddev.run.start"),
                                () -> finishRenameProject(project, workingDirectory, snapshotName, hasDatabase,
                                        afterCompletion))));

        if (hasDatabase) {
            runner.runOnSuccess(this.createCommandLine("snapshot", project, workingDirectory)
                            .withParameters("--name", snapshotName),
                    DdevIntegrationBundle.message("ddev.run.renameProject.snapshot"), stopOldProject);
        } else {
            stopOldProject.run();
        }
    }

    private void finishRenameProject(@NotNull Project project, @Nullable String workingDirectory,
                                     @NotNull String snapshotName, boolean hasDatabase,
                                     @Nullable Runnable afterCompletion) {
        final Runnable complete = () -> {
            if (workingDirectory == null || Objects.equals(workingDirectory, project.getBasePath())) {
                this.updateConfiguration(project);
            }
            this.runAfterTargetCommand(project, workingDirectory, afterCompletion);
        };

        if (!hasDatabase) {
            complete.run();
            return;
        }

        Runner.getInstance(project).runOnSuccess(this.createCommandLine("snapshot", project, workingDirectory)
                        .withParameters("restore", snapshotName),
                DdevIntegrationBundle.message("ddev.run.renameProject.restore"), () -> {
                    final String projectRoot = workingDirectory != null ? workingDirectory : project.getBasePath();
                    if (projectRoot != null) {
                        try {
                            SnapshotFileManager.deleteSnapshot(Path.of(projectRoot), snapshotName);
                        } catch (IOException exception) {
                            LOG.warn("Unable to remove temporary rename snapshot " + snapshotName, exception);
                        }
                    }
                    complete.run();
                });
    }

    @Override
    public void createProject(@NotNull Project project, @NotNull String workingDirectory, @Nullable Runnable afterCompletion, @NotNull String... configArguments) {
        final String title = DdevIntegrationBundle.message("ddev.run.config");
        final Runner runner = Runner.getInstance(project);
        final GeneralCommandLine commandLine = this.createCommandLine("config", project, workingDirectory).withParameters(configArguments);
        runner.runOnSuccess(commandLine, title, afterCompletion);
    }

    @Override
    public void updateConfig(@NotNull Project project, @NotNull String... configArguments) {
        this.updateConfig(project, null, null, configArguments);
    }

    @Override
    public void updateConfig(@NotNull Project project, @Nullable String workingDirectory, @Nullable Runnable afterCompletion,
                             @NotNull String... configArguments) {
        final String title = DdevIntegrationBundle.message("ddev.run.config");
        final Runner runner = Runner.getInstance(project);
        runner.runOnSuccess(this.createCommandLine("config", project, workingDirectory).withParameters(configArguments), title, () -> {
            if (workingDirectory == null || Objects.equals(workingDirectory, project.getBasePath())) {
                this.updateConfiguration(project);
            }

            final String restartTitle = DdevIntegrationBundle.message("ddev.run.restart");
            runner.runOnSuccess(this.createCommandLine("restart", project, workingDirectory), restartTitle,
                    () -> this.runAfterTargetCommand(project, workingDirectory, afterCompletion));
        });
    }

    private void runAfterProjectCommand(@NotNull Project project, @Nullable Runnable afterCompletion) {
        this.updateDescription(project);

        if (afterCompletion != null) {
            afterCompletion.run();
        }
    }

    private void runAfterTargetCommand(@NotNull Project project, @Nullable String workingDirectory,
                                       @Nullable Runnable afterCompletion) {
        if (workingDirectory == null || Objects.equals(workingDirectory, project.getBasePath())) {
            this.updateDescription(project);
        }

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

    private void deleteDdevFolderIfConfigured(@NotNull Project project, @Nullable String workingDirectory) {
        if (!DdevSettingsState.getInstance(project).deleteDdevFolderOnDelete || workingDirectory == null) {
            return;
        }

        try {
            DdevProjectFiles.deleteDdevConfig(Path.of(workingDirectory));
        } catch (IOException ignored) {
            // The directory may already be absent or partially removed; DDEV deletion itself still succeeded.
        }
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
