package de.php_perfect.intellij.ddev;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import de.php_perfect.intellij.ddev.error_reporting.SentrySdkInitializer;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class InitPluginActivity implements ProjectActivity, DumbAware {
    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        SentrySdkInitializer.init();
        ApplicationManager.getApplication().executeOnPooledThread(() -> DdevStateManager.getInstance(project).initialize());
        return null;
    }
}
