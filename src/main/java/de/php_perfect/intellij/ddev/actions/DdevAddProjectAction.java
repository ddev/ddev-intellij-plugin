package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.DdevRunner;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import de.php_perfect.intellij.ddev.util.DdevProjectOpener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Registers a directory as a new DDEV project by running {@code ddev config} in it,
 * mirroring the "Add New Project" feature of the VS Code extension.
 */
public final class DdevAddProjectAction extends DdevRunAction {
    private static final String AUTODETECT = DdevIntegrationBundle.message("addProject.type.autodetect");

    private static final List<String> PROJECT_TYPES = List.of(
            AUTODETECT, "backdrop", "cakephp", "codeigniter", "craftcms", "drupal", "drupal7", "drupal8", "drupal9",
            "drupal10", "drupal11", "generic", "joomla", "laravel", "magento", "magento2", "php", "shopware6",
            "silverstripe", "symfony", "typo3", "wordpress", "wp-bedrock"
    );

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

        JBPopupFactory.getInstance()
                .createPopupChooserBuilder(PROJECT_TYPES)
                .setTitle(DdevIntegrationBundle.message("addProject.type.title"))
                .setNamerForFiltering(type -> type)
                .setFilterAlwaysVisible(true)
                .setItemChosenCallback(type -> createProject(project, directory.getPath(), projectName.trim(), type))
                .createPopup()
                .showCenteredInCurrentWindow(project);
    }

    private static void createProject(@NotNull Project project, @NotNull String directory, @NotNull String projectName, @NotNull String type) {
        final List<String> arguments = new ArrayList<>();
        arguments.add("--project-name=" + projectName);

        if (!AUTODETECT.equals(type)) {
            arguments.add("--project-type=" + type);
        }

        DdevRunner.getInstance().createProject(project, directory, () -> ApplicationManager.getApplication().invokeLater(() -> {
            final boolean openProject = MessageDialogBuilder.yesNo(
                    DdevIntegrationBundle.message("addProject.open.title"),
                    DdevIntegrationBundle.message("addProject.open.message", projectName)
            ).ask(project);

            if (openProject) {
                DdevProjectOpener.openInNewWindow(directory);
            }
        }), arguments.toArray(new String[0]));
    }

    @Override
    protected boolean isActive(@NotNull Project project) {
        final State state = DdevStateManager.getInstance(project).getState();

        return state.isAvailable();
    }
}
