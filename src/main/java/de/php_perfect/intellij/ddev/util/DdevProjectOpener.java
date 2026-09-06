package de.php_perfect.intellij.ddev.util;

import com.intellij.ide.impl.ProjectUtil;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Opens a directory as an IDE project.
 */
public final class DdevProjectOpener {
    private DdevProjectOpener() {
    }

    public static void openInCurrentWindow(@NotNull String path) {
        ProjectUtil.openOrImport(Path.of(path), null, false);
    }

    public static void openInNewWindow(@NotNull String path) {
        ProjectUtil.openOrImport(Path.of(path), null, true);
    }
}
