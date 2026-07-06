package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.DdevRunner;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public final class DdevExportDatabaseAction extends DdevRunningAction {
    @Override
    protected void run(@NotNull Project project) {
        final FileSaverDescriptor descriptor = new FileSaverDescriptor(
                DdevIntegrationBundle.message("dialog.exportDatabase.title"),
                DdevIntegrationBundle.message("dialog.exportDatabase.description")
        );

        final VirtualFileWrapper fileWrapper = FileChooserFactory.getInstance()
                .createSaveFileDialog(descriptor, project)
                .save((Path) null, "db.sql.gz");

        if (fileWrapper != null) {
            DdevRunner.getInstance().exportDatabase(project, fileWrapper.getFile().getAbsolutePath());
        }
    }
}
