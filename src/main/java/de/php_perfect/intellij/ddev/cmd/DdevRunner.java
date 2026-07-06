package de.php_perfect.intellij.ddev.cmd;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface DdevRunner {

    void start(@NotNull Project project);

    void restart(@NotNull Project project);

    void stop(@NotNull Project project);

    void powerOff(@NotNull Project project);

    void delete(@NotNull Project project);

    void share(@NotNull Project project);

    void stopShare(@NotNull Project project);

    void config(@NotNull Project project);

    void createSnapshot(@NotNull Project project);

    void createSnapshot(@NotNull Project project, @Nullable String workingDirectory);

    void restoreSnapshot(@NotNull Project project, @NotNull String snapshotName);

    void restoreSnapshot(@NotNull Project project, @Nullable String workingDirectory, @NotNull String snapshotName);

    void clearSnapshots(@NotNull Project project);

    void importDatabase(@NotNull Project project, @NotNull String filePath);

    void importDatabase(@NotNull Project project, @Nullable String workingDirectory, @NotNull String filePath);

    void exportDatabase(@NotNull Project project, @NotNull String filePath);

    void exportDatabase(@NotNull Project project, @Nullable String workingDirectory, @NotNull String filePath);

    void enableXdebug(@NotNull Project project);

    void disableXdebug(@NotNull Project project);

    void mutagenReset(@NotNull Project project);

    void deleteImages(@NotNull Project project);

    void installAddOn(@NotNull Project project, @NotNull String addOnName);

    void removeAddOn(@NotNull Project project, @NotNull String addOnName);

    void startProject(@NotNull Project project, @NotNull String projectName, @Nullable Runnable afterCompletion);

    void stopProject(@NotNull Project project, @NotNull String projectName, @Nullable Runnable afterCompletion);

    void restartProject(@NotNull Project project, @NotNull String projectName, @Nullable Runnable afterCompletion);

    void stopProjects(@NotNull Project project, @NotNull List<String> projectNames, @Nullable Runnable afterCompletion);

    void deleteProject(@NotNull Project project, @NotNull String projectName, @Nullable Runnable afterCompletion);

    void renameProject(@NotNull Project project, @Nullable String workingDirectory, @NotNull String newName, @Nullable Runnable afterCompletion);

    void createProject(@NotNull Project project, @NotNull String workingDirectory, @Nullable Runnable afterCompletion, @NotNull String... configArguments);

    void updateConfig(@NotNull Project project, @NotNull String... configArguments);

    static DdevRunner getInstance() {
        return ApplicationManager.getApplication().getService(DdevRunner.class);
    }
}
