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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Updates DDEV through Homebrew when it was installed that way
 * (https://github.com/ddev/ddev-intellij-plugin/issues/40). Other installation
 * methods keep the "Installation Instructions" link as their update path.
 */
public final class UpdateDdevAction extends DumbAwareAction {
    private static final @NotNull List<String> BREW_PATHS = List.of(
            "/opt/homebrew/bin/brew",
            "/usr/local/bin/brew",
            "/home/linuxbrew/.linuxbrew/bin/brew"
    );

    public UpdateDdevAction() {
        super(DdevIntegrationBundle.messagePointer("action.DdevIntegration.UpdateDdev.text"), DdevIntegrationBundle.messagePointer("action.DdevIntegration.UpdateDdev.description"), AllIcons.Actions.Download);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        final String brewBinary = findBrewManagingDdev();

        if (project == null || brewBinary == null) {
            return;
        }

        final GeneralCommandLine commandLine = new GeneralCommandLine(brewBinary, "upgrade", "ddev/ddev/ddev");
        final String title = DdevIntegrationBundle.message("ddev.run.updateDdev");

        Runner.getInstance(project).run(commandLine, title, () ->
                ApplicationManager.getApplication().executeOnPooledThread(() -> DdevStateManager.getInstance(project).reinitialize()));
    }

    public static boolean isAvailable() {
        return findBrewManagingDdev() != null;
    }

    private static @Nullable String findBrewManagingDdev() {
        for (final String brewPath : BREW_PATHS) {
            final Path brew = Paths.get(brewPath);

            if (!Files.isExecutable(brew)) {
                continue;
            }

            // brew lives at <prefix>/bin/brew; DDEV installed via Homebrew has a keg at <prefix>/Cellar/ddev.
            final Path cellar = brew.getParent().getParent().resolve("Cellar").resolve("ddev");

            if (Files.isDirectory(cellar)) {
                return brewPath;
            }
        }

        return null;
    }
}
