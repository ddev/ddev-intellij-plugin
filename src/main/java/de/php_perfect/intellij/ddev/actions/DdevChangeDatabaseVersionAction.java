package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.DdevRunner;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class DdevChangeDatabaseVersionAction extends DdevRunAction {
    private static final List<String> DATABASES = List.of(
            "mariadb:11.8", "mariadb:11.4", "mariadb:10.11", "mariadb:10.6",
            "mysql:8.4", "mysql:8.0", "mysql:5.7",
            "postgres:18", "postgres:17", "postgres:16", "postgres:15", "postgres:14"
    );

    @Override
    protected void run(@NotNull Project project) {
        JBPopupFactory.getInstance()
                .createPopupChooserBuilder(DATABASES)
                .setTitle(DdevIntegrationBundle.message("changeDatabase.popupTitle"))
                .setItemChosenCallback(database -> {
                    final boolean confirmed = MessageDialogBuilder.yesNo(
                            DdevIntegrationBundle.message("changeDatabase.confirm.title"),
                            DdevIntegrationBundle.message("changeDatabase.confirm.message", database)
                    ).ask(project);

                    if (confirmed) {
                        DdevRunner.getInstance().updateConfig(project, "--database=" + database);
                    }
                })
                .createPopup()
                .showCenteredInCurrentWindow(project);
    }

    @Override
    protected boolean isActive(@NotNull Project project) {
        final State state = DdevStateManager.getInstance(project).getState();

        return state.isAvailable() && state.isConfigured();
    }
}
