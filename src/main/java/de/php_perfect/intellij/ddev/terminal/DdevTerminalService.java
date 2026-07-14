package de.php_perfect.intellij.ddev.terminal;

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
}
