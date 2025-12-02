package de.php_perfect.intellij.ddev.cmd;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface DdevRunner {

    void start(@NotNull Project project);

    void restart(@NotNull Project project);

    void stop(@NotNull Project project);

    void powerOff(@NotNull Project project);

    void delete(@NotNull Project project);

    void share(@NotNull Project project);

    void config(@NotNull Project project);

    /**
     * Gets a list of available add-ons that can be added to a DDEV project.
     *
     * @param project The project to get available add-ons for
     * @return A list of available add-ons
     */
    @NotNull List<DdevAddon> getAvailableAddons(@NotNull Project project);

    /**
     * Gets a list of add-ons currently installed in a DDEV project.
     *
     * @param project The project to get installed add-ons for
     * @return A list of installed add-ons
     */
    @NotNull List<DdevAddon> getInstalledAddons(@NotNull Project project);

    /**
     * Adds an add-on to a DDEV project.
     *
     * @param project The project to add the add-on to
     * @param addon   The add-on to add
     */
    void addAddon(@NotNull Project project, @NotNull DdevAddon addon);

    /**
     * Removes an add-on from a DDEV project.
     *
     * @param project The project to remove the add-on from
     * @param addon   The add-on to remove
     */
    void deleteAddon(@NotNull Project project, @NotNull DdevAddon addon);

    static DdevRunner getInstance() {
        return ApplicationManager.getApplication().getService(DdevRunner.class);
    }
}
