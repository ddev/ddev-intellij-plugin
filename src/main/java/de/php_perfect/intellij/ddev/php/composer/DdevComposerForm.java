package de.php_perfect.intellij.ddev.php.composer;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import com.jetbrains.php.composer.execution.ComposerExecution;
import com.jetbrains.php.composer.execution.ComposerExecutionProvider;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Configuration form for DDEV Composer execution.
 */
public class DdevComposerForm implements ComposerExecutionProvider.Form {
    private final JPanel myMainPanel;
    private DdevComposerExecution myExecution;

    @SuppressWarnings({"unused", "UnusedParameters"}) // Parameters required by ComposerExecutionProvider interface
    public DdevComposerForm(@NotNull Project project, @NotNull Disposable disposable) {
        myExecution = new DdevComposerExecution();
        myMainPanel = createUIComponents();
    }

    private JPanel createUIComponents() {
        JBLabel descriptionLabel = new JBLabel(DdevIntegrationBundle.message("composer.form.ddev.description"));
        descriptionLabel.setFont(descriptionLabel.getFont().deriveFont(Font.ITALIC));

        return FormBuilder.createFormBuilder()
                .addComponent(descriptionLabel)
                .getPanel();
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return myMainPanel;
    }

    @Nullable
    @Override
    public ValidationInfo validate() {
        // No validation needed for DDEV composer
        return null;
    }

    @Override
    public boolean reset(@NotNull ComposerExecution execution) {
        if (execution instanceof DdevComposerExecution ddevExecution) {
            myExecution = ddevExecution;
            return true;
        }
        return false;
    }

    @Override
    public void apply() {
        // No additional configuration needed for DDEV
    }



    @NotNull
    @Override
    public ComposerExecution getExecution() {
        return myExecution;
    }

    @Override
    public boolean isModified(@NotNull ComposerExecution execution) {
        // Check if the current execution is different from the form's execution
        return !(execution instanceof DdevComposerExecution) || !myExecution.equals(execution);
    }
}
