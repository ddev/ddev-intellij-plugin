package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.DdevRunner;
import org.jetbrains.annotations.NotNull;

public final class DdevImportDatabaseAction extends DdevRunningAction {
    @Override
    protected void run(@NotNull Project project) {
        final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.singleFile()
                .withTitle(DdevIntegrationBundle.message("dialog.importDatabase.title"))
                .withDescription(DdevIntegrationBundle.message("dialog.importDatabase.description"));

        final VirtualFile file = FileChooser.chooseFile(descriptor, project, null);

        if (file != null) {
            DdevRunner.getInstance().importDatabase(project, file.getPath());
        }
    }
}
