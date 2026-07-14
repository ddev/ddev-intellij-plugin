package de.php_perfect.intellij.ddev.actions;

import de.php_perfect.intellij.ddev.wordpress.WordPressConfigManager;
import org.jetbrains.annotations.NotNull;

public final class DdevWordPressDisableSilentDebugAction extends DdevWordPressDebugAction {
    public DdevWordPressDisableSilentDebugAction() {
        super(WordPressConfigManager.DebugMode.DISABLED);
    }

    @Override
    protected boolean isApplicable(@NotNull WordPressConfigManager.DebugState state) {
        return state.silent();
    }
}
