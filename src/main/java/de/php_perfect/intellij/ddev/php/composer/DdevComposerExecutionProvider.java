package de.php_perfect.intellij.ddev.php.composer;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.jetbrains.php.composer.execution.ComposerExecution;
import com.jetbrains.php.composer.execution.ComposerExecutionProvider;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import org.jdom.Element;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides DDEV-based Composer execution for projects using DDEV.
 */
public class DdevComposerExecutionProvider implements ComposerExecutionProvider {

    @Nls
    @NotNull
    @Override
    public String getPresentableName() {
        return DdevIntegrationBundle.message("composer.execution.provider.ddev.name");
    }

    @Override
    public boolean isMyExecution(@NotNull ComposerExecution execution) {
        return execution instanceof DdevComposerExecution;
    }

    @NotNull
    @Override
    public Form createForm(@NotNull Project project, @NotNull Disposable disposable) {
        return new DdevComposerForm(project, disposable);
    }

    @Nullable
    @Override
    public DdevComposerExecution loadExecution(@NotNull Element element) {
        return DdevComposerExecution.load(element);
    }
}
