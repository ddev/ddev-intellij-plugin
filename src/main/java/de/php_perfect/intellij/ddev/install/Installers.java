package de.php_perfect.intellij.ddev.install;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.Runner;
import de.php_perfect.intellij.ddev.cmd.ShareProviders;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.terminal.DdevTerminalService;
import de.php_perfect.intellij.ddev.util.Homebrew;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Installs DDEV and the tunnel binaries used by {@code ddev share}. Homebrew is used where it is
 * available; otherwise DDEV's install script runs in a terminal so the user can answer its sudo
 * prompt, and anything else falls back to the vendor's installation page.
 */
public final class Installers {
    public static final @NotNull String DDEV_INSTALL_DOCS = "https://ddev.readthedocs.io/en/stable/users/install/ddev-installation/";
    private static final @NotNull String DDEV_INSTALL_SCRIPT = "curl -fsSL https://ddev.com/install.sh | bash";

    private Installers() {
    }

    public static void installDdev(@NotNull Project project) {
        final String brew = Homebrew.find();

        if (brew != null) {
            Runner.getInstance(project).runOnSuccess(new GeneralCommandLine(brew, "install", "ddev/ddev/ddev"),
                    DdevIntegrationBundle.message("ddev.run.installDdev"),
                    () -> ApplicationManager.getApplication().executeOnPooledThread(
                            () -> DdevStateManager.getInstance(project).reinitialize()));
            return;
        }

        final DdevTerminalService terminal = DdevTerminalService.getInstance(project);
        if (!SystemInfo.isWindows && terminal != null) {
            terminal.openCommand(List.of("bash", "-c", DDEV_INSTALL_SCRIPT),
                    DdevIntegrationBundle.message("ddev.run.installDdev"), System.getProperty("user.home"));
            return;
        }

        BrowserUtil.browse(DDEV_INSTALL_DOCS);
    }

    public static boolean canInstallShareTool(@NotNull String tool) {
        return Homebrew.find() != null && ShareProviders.homebrewFormula(tool) != null;
    }

    public static void installShareTool(@NotNull Project project, @NotNull String tool) {
        final String brew = Homebrew.find();
        final String formula = ShareProviders.homebrewFormula(tool);

        if (brew == null || formula == null) {
            final String url = ShareProviders.installUrl(tool);
            if (url != null) {
                BrowserUtil.browse(url);
            }
            return;
        }

        Runner.getInstance(project).run(new GeneralCommandLine(brew, "install", formula),
                DdevIntegrationBundle.message("ddev.run.installTool", tool));
    }
}
