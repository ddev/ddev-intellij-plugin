package de.php_perfect.intellij.ddev.actions;

import de.php_perfect.intellij.ddev.wordpress.WordPressConfigManager;
import org.jetbrains.annotations.NotNull;

public final class DdevWordPressEnableSilentDebugAction extends DdevWordPressDebugAction {
    public DdevWordPressEnableSilentDebugAction() {
        super(WordPressConfigManager.DebugMode.SILENT);
    }

    @Override
    protected boolean isApplicable(@NotNull WordPressConfigManager.DebugState state) {
        return !state.silent();
    }
}
