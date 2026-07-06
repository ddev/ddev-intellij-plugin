package de.php_perfect.intellij.ddev.cmd;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.version.Version;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface Ddev {
    @NotNull Version version(@NotNull String binary, @NotNull Project project) throws CommandFailedException;

    @NotNull Versions detailedVersions(@NotNull String binary, @NotNull Project project) throws CommandFailedException;

    @NotNull Description describe(@NotNull String binary, @NotNull Project project) throws CommandFailedException;

    @NotNull Description describeProject(@NotNull String binary, @NotNull Project project, @NotNull String projectName) throws CommandFailedException;

    @NotNull List<AddOn> listAddOns(@NotNull String binary, @NotNull Project project) throws CommandFailedException;

    @NotNull List<InstalledAddOn> listInstalledAddOns(@NotNull String binary, @NotNull Project project) throws CommandFailedException;

    @NotNull List<DdevProject> listProjects(@NotNull String binary, @NotNull Project project) throws CommandFailedException;

    @NotNull List<Snapshot> listSnapshots(@NotNull String binary, @NotNull Project project) throws CommandFailedException;

    @NotNull List<Snapshot> listSnapshots(@NotNull String binary, @NotNull Project project, @Nullable String workingDirectory) throws CommandFailedException;

    static Ddev getInstance() {
        return ApplicationManager.getApplication().getService(Ddev.class);
    }
}
