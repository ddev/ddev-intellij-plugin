package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.DdevConfigOptions;
import de.php_perfect.intellij.ddev.cmd.DdevConfigOptionsLoader;
import de.php_perfect.intellij.ddev.cmd.DdevRunner;
import de.php_perfect.intellij.ddev.cmd.Ddev;
import de.php_perfect.intellij.ddev.cmd.Description;
import de.php_perfect.intellij.ddev.cmd.CommandFailedException;
import de.php_perfect.intellij.ddev.cms.CmsInstallationRecipe;
import de.php_perfect.intellij.ddev.cms.CmsProjectInstaller;
import de.php_perfect.intellij.ddev.settings.DdevSettingsState;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import de.php_perfect.intellij.ddev.util.DdevProjectOpener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

/**
 * Registers a directory as a new DDEV project by running {@code ddev config} in it,
 * mirroring the "Add New Project" feature of the VS Code extension.
 */
public final class DdevAddProjectAction extends DdevRunAction {
    private static final String AUTODETECT = DdevIntegrationBundle.message("addProject.type.autodetect");
    private static final String INSTALL_CMS = DdevIntegrationBundle.message("addProject.type.installCms");

    @Override
    protected void run(@NotNull Project project) {
        final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.singleDir()
                .withTitle(DdevIntegrationBundle.message("addProject.chooseDirectory.title"))
                .withDescription(DdevIntegrationBundle.message("addProject.chooseDirectory.description"));

        final VirtualFile directory = FileChooser.chooseFile(descriptor, project, null);

        if (directory == null) {
            return;
        }

        final String defaultName = directory.getName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]", "-");
        final String projectName = Messages.showInputDialog(
                project,
                DdevIntegrationBundle.message("addProject.name.message"),
                DdevIntegrationBundle.message("addProject.name.title"),
                null,
                defaultName,
                null
        );

        if (projectName == null || projectName.isBlank()) {
            return;
        }

        new Task.Backgroundable(project, DdevIntegrationBundle.message("configOptions.loading"), true) {
            private DdevConfigOptions options;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                this.options = DdevConfigOptionsLoader.getInstance().load(indicator);
            }

            @Override
            public void onSuccess() {
                final List<String> projectTypes = new ArrayList<>();
                projectTypes.add(AUTODETECT);
                projectTypes.add(INSTALL_CMS);
                projectTypes.addAll(this.options.projectTypes());

                JBPopupFactory.getInstance()
                        .createPopupChooserBuilder(projectTypes)
                        .setTitle(DdevIntegrationBundle.message("addProject.type.title"))
                        .setNamerForFiltering(type -> type)
                        .setFilterAlwaysVisible(true)
                        .setItemChosenCallback(type -> {
                            if (INSTALL_CMS.equals(type)) {
                                chooseCmsRecipe(project, directory.getPath(), projectName.trim());
                            } else {
                                final CmsInstallationRecipe automaticRecipe =
                                        DdevSettingsState.getInstance(project).automaticallyInstallCms
                                                ? CmsInstallationRecipe.forAutomaticProjectType(type)
                                                : null;
                                createProject(project, directory.getPath(), projectName.trim(), type, automaticRecipe);
                            }
                        })
                        .createPopup()
                        .showCenteredInCurrentWindow(project);
            }
        }.queue();
    }

    private static void chooseCmsRecipe(@NotNull Project project, @NotNull String directory,
                                        @NotNull String projectName) {
        final List<CmsInstallationRecipe> recipes = CmsInstallationRecipe.all();
        JBPopupFactory.getInstance().createPopupChooserBuilder(recipes)
                .setTitle(DdevIntegrationBundle.message("cms.install.choose.title"))
                .setRenderer(new com.intellij.ui.SimpleListCellRenderer<>() {
                    @Override
                    public void customize(@NotNull javax.swing.JList<? extends CmsInstallationRecipe> list,
                                          CmsInstallationRecipe value, int index, boolean selected,
                                          boolean hasFocus) {
                        this.setText(value.displayName());
                    }
                })
                .setNamerForFiltering(CmsInstallationRecipe::displayName)
                .setFilterAlwaysVisible(true)
                .setItemChosenCallback(recipe -> createProject(project, directory, projectName,
                        recipe.projectType(), recipe))
                .createPopup().showCenteredInCurrentWindow(project);
    }

    private static void createProject(@NotNull Project project, @NotNull String directory,
                                      @NotNull String projectName, @NotNull String type,
                                      CmsInstallationRecipe requestedRecipe) {
        final List<String> arguments = new ArrayList<>();
        arguments.add("--project-name=" + projectName);
        CmsInstallationRecipe recipe = requestedRecipe;

        if (recipe != null && !isEmptyDirectory(directory)) {
            final boolean configureOnly = MessageDialogBuilder.yesNo(
                    DdevIntegrationBundle.message("cms.install.nonEmpty.title"),
                    DdevIntegrationBundle.message("cms.install.nonEmpty.message")
            ).ask(project);

            if (!configureOnly) {
                return;
            }

            recipe = null;
        }

        if (recipe != null) {
            arguments.addAll(recipe.configArguments());
        } else if (!AUTODETECT.equals(type)) {
            arguments.add("--project-type=" + type);
        }

        final CmsInstallationRecipe selectedRecipe = recipe;
        final CmsProjectInstaller.Credentials credentials = selectedRecipe != null && selectedRecipe.requiresCredentials()
                ? requestCredentials(project)
                : CmsProjectInstaller.Credentials.NONE;

        if (credentials == null) {
            return;
        }

        DdevRunner.getInstance().createProject(project, directory, () -> {
            if (selectedRecipe != null) {
                CmsProjectInstaller.install(project, directory, projectName, selectedRecipe, credentials,
                        () -> askAfterCmsInstall(project, directory, projectName, selectedRecipe, credentials));
            } else {
                askToOpenProject(project, directory, projectName);
            }
        }, arguments.toArray(new String[0]));
    }

    private static void askAfterCmsInstall(@NotNull Project project, @NotNull String directory,
                                           @NotNull String projectName,
                                           @NotNull CmsInstallationRecipe recipe,
                                           @NotNull CmsProjectInstaller.Credentials credentials) {
        ApplicationManager.getApplication().invokeLater(() -> {
            final String[] options = {
                    DdevIntegrationBundle.message("cms.install.result.openBrowser"),
                    DdevIntegrationBundle.message("cms.install.result.openIde"),
                    DdevIntegrationBundle.message("cms.install.result.close")
            };
            final int choice = Messages.showDialog(project,
                    DdevIntegrationBundle.message("cms.install.result.message", recipe.displayName()),
                    DdevIntegrationBundle.message("cms.install.result.title"), options, 0,
                    Messages.getInformationIcon());
            if (choice == 0) {
                openInstalledSite(project, projectName, recipe, credentials);
            } else if (choice == 1) {
                DdevProjectOpener.openInNewWindow(directory);
            }
        });
    }

    private static void openInstalledSite(@NotNull Project project, @NotNull String projectName,
                                          @NotNull CmsInstallationRecipe recipe,
                                          @NotNull CmsProjectInstaller.Credentials credentials) {
        final String binary = DdevStateManager.getInstance(project).getState().getDdevBinary();
        if (binary == null) {
            return;
        }

        new Task.Backgroundable(project, DdevIntegrationBundle.message("cms.install.result.loadingUrl"), true) {
            private String url;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    final Description description = Ddev.getInstance().describeProject(binary, project, projectName);
                    if (description.getPrimaryUrl() != null) {
                        this.url = recipe.firstLaunchUrl(description.getPrimaryUrl(), credentials.username());
                    }
                } catch (CommandFailedException ignored) {
                    this.url = null;
                }
            }

            @Override
            public void onSuccess() {
                if (this.url != null) {
                    BrowserUtil.browse(this.url);
                } else {
                    Messages.showErrorDialog(project,
                            DdevIntegrationBundle.message("cms.install.primaryUrl.failed"),
                            DdevIntegrationBundle.message("cms.install.failed.title"));
                }
            }
        }.queue();
    }

    private static void askToOpenProject(@NotNull Project project, @NotNull String directory,
                                         @NotNull String projectName) {
        ApplicationManager.getApplication().invokeLater(() -> {
            final boolean openProject = MessageDialogBuilder.yesNo(
                    DdevIntegrationBundle.message("addProject.open.title"),
                    DdevIntegrationBundle.message("addProject.open.message", projectName)
            ).ask(project);

            if (openProject) {
                DdevProjectOpener.openInNewWindow(directory);
            }
        });
    }

    private static CmsProjectInstaller.Credentials requestCredentials(@NotNull Project project) {
        final String username = Messages.showInputDialog(project,
                DdevIntegrationBundle.message("cms.install.credentials.username"),
                DdevIntegrationBundle.message("cms.install.credentials.title"), null, "admin", null);
        if (username == null || username.isBlank()) {
            return null;
        }

        final String password = Messages.showPasswordDialog(project,
                DdevIntegrationBundle.message("cms.install.credentials.password"),
                DdevIntegrationBundle.message("cms.install.credentials.title"), null);
        if (password == null || password.isBlank()) {
            return null;
        }

        final String email = Messages.showInputDialog(project,
                DdevIntegrationBundle.message("cms.install.credentials.email"),
                DdevIntegrationBundle.message("cms.install.credentials.title"), null,
                "admin@example.com", null);
        if (email == null || email.isBlank()) {
            return null;
        }

        return new CmsProjectInstaller.Credentials(username.trim(), password, email.trim());
    }

    private static boolean isEmptyDirectory(@NotNull String directory) {
        try (var entries = Files.list(Path.of(directory))) {
            return entries.findAny().isEmpty();
        } catch (IOException exception) {
            return false;
        }
    }

    @Override
    protected boolean isActive(@NotNull Project project) {
        final State state = DdevStateManager.getInstance(project).getState();

        return state.isAvailable();
    }
}
