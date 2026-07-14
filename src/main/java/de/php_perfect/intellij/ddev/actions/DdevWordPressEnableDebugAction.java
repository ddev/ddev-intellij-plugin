package de.php_perfect.intellij.ddev.actions;

import de.php_perfect.intellij.ddev.wordpress.WordPressConfigManager;
import org.jetbrains.annotations.NotNull;

public final class DdevWordPressEnableDebugAction extends DdevWordPressDebugAction {
    public DdevWordPressEnableDebugAction() {
        super(WordPressConfigManager.DebugMode.ENABLED);
    }

    @Override
    protected boolean isApplicable(@NotNull WordPressConfigManager.DebugState state) {
        return !state.enabled();
    }
}
