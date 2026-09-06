package de.php_perfect.intellij.ddev.actions;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.Runner;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.util.Homebrew;
import de.php_perfect.intellij.ddev.install.Installers;
import de.php_perfect.intellij.ddev.terminal.DdevTerminalService;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Updates DDEV through Homebrew when it was installed that way
 * (https://github.com/ddev/ddev-intellij-plugin/issues/40), or through DDEV's install script when
 * it was installed by that script. Other installation methods keep the "Installation Instructions"
 * link as their update path.
 */
public final class UpdateDdevAction extends DumbAwareAction {
    private static final @NotNull String SCRIPT_INSTALL_LOCATION = "/usr/local/bin/ddev";

    public UpdateDdevAction() {
        super(DdevIntegrationBundle.messagePointer("action.DdevIntegration.UpdateDdev.text"), DdevIntegrationBundle.messagePointer("action.DdevIntegration.UpdateDdev.description"), AllIcons.Actions.Download);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();

        if (project == null) {
            return;
        }

        final String brewBinary = Homebrew.findManagingDdev();

        if (brewBinary == null) {
            if (isScriptInstalled(project)) {
                Installers.installDdev(project);
            }
            return;
        }

        final GeneralCommandLine commandLine = new GeneralCommandLine(brewBinary, "upgrade", "ddev/ddev/ddev");
        final String title = DdevIntegrationBundle.message("ddev.run.updateDdev");

        Runner.getInstance(project).run(commandLine, title, () ->
                ApplicationManager.getApplication().executeOnPooledThread(() -> DdevStateManager.getInstance(project).reinitialize()));
    }

    public static boolean isAvailable(@NotNull Project project) {
        return Homebrew.findManagingDdev() != null || isScriptInstalled(project);
    }

    /**
     * DDEV's install script places the binary in /usr/local/bin; running the script again updates it.
     * Package-manager installs elsewhere keep their own update path.
     */
    private static boolean isScriptInstalled(@NotNull Project project) {
        return !SystemInfo.isWindows && Homebrew.find() == null
                && DdevTerminalService.getInstance(project) != null
                && SCRIPT_INSTALL_LOCATION.equals(DdevStateManager.getInstance(project).getState().getDdevBinary());
    }
}
