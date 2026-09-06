package de.php_perfect.intellij.ddev.dbmanager;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.CommandFailedException;
import de.php_perfect.intellij.ddev.cmd.Ddev;
import de.php_perfect.intellij.ddev.cmd.DdevRunner;
import de.php_perfect.intellij.ddev.cmd.Description;
import de.php_perfect.intellij.ddev.cmd.Service;
import de.php_perfect.intellij.ddev.notification.DdevNotifier;
import de.php_perfect.intellij.ddev.settings.DdevSettingsState;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Opens a project's database with the configured {@link DatabaseManager}. A {@code null} project
 * name and working directory target the IDE project; otherwise that DDEV project is used.
 */
public final class DatabaseOpener {
    private static final @NotNull String DATABASE_TOOL_WINDOW = "Database";

    private DatabaseOpener() {
    }

    public static void open(@NotNull Project project, @Nullable String projectName, @Nullable String workingDirectory) {
        final DatabaseManager manager = DatabaseManager.fromValue(DdevSettingsState.getInstance(project).databaseManager);

        if (manager == null || !manager.isSupported()) {
            chooseAndOpen(project, projectName, workingDirectory);
            return;
        }

        open(project, projectName, workingDirectory, manager);
    }

    public static void chooseAndOpen(@NotNull Project project, @Nullable String projectName, @Nullable String workingDirectory) {
        JBPopupFactory.getInstance()
                .createPopupChooserBuilder(DatabaseManager.available())
                .setTitle(DdevIntegrationBundle.message("database.open.popupTitle"))
                .setItemChosenCallback(manager -> open(project, projectName, workingDirectory, manager))
                .createPopup()
                .showCenteredInCurrentWindow(project);
    }

    public static void open(@NotNull Project project, @Nullable String projectName, @Nullable String workingDirectory,
                            @NotNull DatabaseManager manager) {
        switch (manager.kind()) {
            case IDE -> openToolWindow(project, manager);
            case HOST_COMMAND -> DdevRunner.getInstance().runHostCommand(project, workingDirectory,
                    java.util.Objects.requireNonNull(manager.command()));
            case ADD_ON -> ApplicationManager.getApplication().executeOnPooledThread(
                    () -> openAddOn(project, projectName, workingDirectory, manager, true));
        }
    }

    private static void openToolWindow(@NotNull Project project, @NotNull DatabaseManager manager) {
        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(DATABASE_TOOL_WINDOW);

        if (toolWindow == null) {
            DdevNotifier.getInstance(project).notifyMissingPlugin("Database Tools and SQL", manager.label());
            return;
        }

        toolWindow.activate(null);
    }

    private static void openAddOn(@NotNull Project project, @Nullable String projectName, @Nullable String workingDirectory,
                                  @NotNull DatabaseManager manager, boolean offerInstall) {
        final Description description = describe(project, projectName);

        if (description == null) {
            return;
        }

        final Service service = description.getServices().get(manager.command());
        final String url = service == null ? null : service.getPreferredUrl();

        if (url != null) {
            BrowserUtil.browse(url);
            return;
        }

        final String name = description.getName();
        if (!offerInstall || name == null) {
            return;
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            if (!MessageDialogBuilder.yesNo(
                    DdevIntegrationBundle.message("database.addOn.install.title", manager.label()),
                    DdevIntegrationBundle.message("database.addOn.install.message", manager.label(), name)
            ).ask(project)) {
                return;
            }

            final DdevRunner runner = DdevRunner.getInstance();
            runner.installAddOn(project, workingDirectory, java.util.Objects.requireNonNull(manager.addOn()),
                    () -> runner.restartProject(project, name, () -> ApplicationManager.getApplication().executeOnPooledThread(
                            () -> openAddOn(project, name, workingDirectory, manager, false))));
        });
    }

    private static @Nullable Description describe(@NotNull Project project, @Nullable String projectName) {
        final String binary = DdevStateManager.getInstance(project).getState().getDdevBinary();

        if (binary == null) {
            return null;
        }

        try {
            return projectName == null
                    ? Ddev.getInstance().describe(binary, project)
                    : Ddev.getInstance().describeProject(binary, project, projectName);
        } catch (CommandFailedException exception) {
            return null;
        }
    }
}
