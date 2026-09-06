package de.php_perfect.intellij.ddev.util;

import com.intellij.ide.impl.OpenProjectTaskBuilder;
import com.intellij.ide.impl.ProjectUtil;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Opens a directory as an IDE project.
 */
public final class DdevProjectOpener {
    private DdevProjectOpener() {
    }

    public static void openInCurrentWindow(@NotNull String path) {
        ProjectUtil.openOrImport(Path.of(path), new OpenProjectTaskBuilder().build(builder -> {
            builder.setForceOpenInNewFrame(false);
            return Unit.INSTANCE;
        }));
    }

    public static void openInNewWindow(@NotNull String path) {
        ProjectUtil.openOrImport(Path.of(path), new OpenProjectTaskBuilder().build(builder -> {
            builder.setForceOpenInNewFrame(true);
            return Unit.INSTANCE;
        }));
    }
}
