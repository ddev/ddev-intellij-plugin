package de.php_perfect.intellij.ddev.status_bar;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.widget.StatusBarEditorBasedWidgetFactory;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public final class DdevStatusBarWidgetFactoryImpl extends StatusBarEditorBasedWidgetFactory {
    @Override
    public @NonNls @NotNull String getId() {
        return DdevStatusBarWidgetImpl.WIDGET_ID;
    }

    @Override
    public @Nls @NotNull String getDisplayName() {
        return DdevIntegrationBundle.message("statusBar.displayName");
    }

    @Override
    public boolean canBeEnabledOn(@NotNull StatusBar statusBar) {
        Project project = statusBar.getProject();
        return project != null;
    }

    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
        return new DdevStatusBarWidgetImpl(project);
    }
}
