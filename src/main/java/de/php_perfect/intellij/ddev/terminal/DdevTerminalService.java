package de.php_perfect.intellij.ddev.terminal;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Optional bridge to the bundled Terminal plugin.
 *
 * <p>The implementation is registered only when the Terminal plugin is available, keeping
 * core project and tool-window classes loadable when that optional dependency is disabled.</p>
 */
public interface DdevTerminalService {
    void open(@NotNull List<String> ddevArguments, @NotNull String title, @Nullable String workingDirectory);

    /**
     * Runs an arbitrary command in a new terminal tab, for steps that need the user's input, such as
     * a sudo password.
     */
    void openCommand(@NotNull List<String> command, @NotNull String title, @Nullable String workingDirectory);

    /**
     * Returns the service, or {@code null} when the Terminal plugin is unavailable.
     */
    static @Nullable DdevTerminalService getInstance(@NotNull Project project) {
        return project.getService(DdevTerminalService.class);
    }
}
