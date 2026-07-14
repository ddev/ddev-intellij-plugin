package de.php_perfect.intellij.ddev.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.DdevConfigFiles;
import de.php_perfect.intellij.ddev.cmd.DdevConfigOptions;
import de.php_perfect.intellij.ddev.cmd.DdevConfigOptionsLoader;
import de.php_perfect.intellij.ddev.cmd.DdevProject;
import de.php_perfect.intellij.ddev.cmd.DdevRunner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

final class DdevConfigurationActions {
    private DdevConfigurationActions() {
    }

    static class Change extends DdevProjectAction {
        private final @NotNull String popupTitle;
        private final @NotNull String argumentPrefix;
        private final @NotNull Function<DdevConfigOptions, List<String>> choices;

        Change(@NotNull DdevProjectsPanel panel, @NotNull String text, @NotNull String popupTitle,
               @NotNull String argumentPrefix, @NotNull Function<DdevConfigOptions, List<String>> choices) {
            super(panel, text, AllIcons.Actions.Edit);
            this.popupTitle = popupTitle;
            this.argumentPrefix = argumentPrefix;
            this.choices = choices;
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return super.isEnabledFor(selected) && selected.getAppRoot() != null;
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            new Task.Backgroundable(this.panel.project(), DdevIntegrationBundle.message("configOptions.loading"), true) {
                private DdevConfigOptions options;

                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    this.options = DdevConfigOptionsLoader.getInstance().load(indicator);
                }

                @Override
                public void onSuccess() {
                    final List<String> values = Change.this.choices.apply(this.options);
                    if (!values.isEmpty()) {
                        JBPopupFactory.getInstance().createPopupChooserBuilder(values)
                                .setTitle(Change.this.popupTitle)
                                .setNamerForFiltering(value -> value)
                                .setFilterAlwaysVisible(true)
                                .setItemChosenCallback(value -> Change.this.apply(selected, value))
                                .createPopup().showCenteredInCurrentWindow(Change.this.panel.project());
                    }
                }
            }.queue();
        }

        protected void apply(@NotNull DdevProject selected, @NotNull String value) {
            DdevRunner.getInstance().updateConfig(this.panel.project(), selected.getAppRoot(),
                    this.panel::refreshLater, this.argumentPrefix + value);
        }
    }

    static final class ChangeNodejs extends Change {
        private static final String CUSTOM = DdevIntegrationBundle.message("changeNodejsVersion.custom");

        ChangeNodejs(@NotNull DdevProjectsPanel panel) {
            super(panel, DdevIntegrationBundle.message("action.DdevIntegration.Run.ChangeNodejsVersion.MainMenu.text"),
                    DdevIntegrationBundle.message("changeNodejsVersion.title"), "--nodejs-version=", options -> {
                        final ArrayList<String> values = new ArrayList<>(options.nodejsVersions());
                        values.add(CUSTOM);
                        return values;
                    });
        }

        @Override
        protected void apply(@NotNull DdevProject selected, @NotNull String value) {
            if (!CUSTOM.equals(value)) {
                super.apply(selected, value);
                return;
            }
            final String custom = Messages.showInputDialog(this.panel.project(),
                    DdevIntegrationBundle.message("changeNodejsVersion.message"),
                    DdevIntegrationBundle.message("changeNodejsVersion.title"), null);
            if (custom != null && !custom.isBlank()) {
                super.apply(selected, custom.trim());
            }
        }
    }

    static final class ChangeDatabase extends Change {
        ChangeDatabase(@NotNull DdevProjectsPanel panel) {
            super(panel,
                    DdevIntegrationBundle.message("action.DdevIntegration.Run.ChangeDatabaseVersion.MainMenu.text"),
                    DdevIntegrationBundle.message("changeDatabase.popupTitle"), "--database=",
                    DdevConfigOptions::databases);
        }

        @Override
        protected void apply(@NotNull DdevProject selected, @NotNull String value) {
            if (MessageDialogBuilder.yesNo(DdevIntegrationBundle.message("changeDatabase.confirm.title"),
                    DdevIntegrationBundle.message("changeDatabase.confirm.message", value))
                    .ask(this.panel.project())) {
                super.apply(selected, value);
            }
        }
    }

    static final class OpenPhp extends DdevProjectAction {
        OpenPhp(@NotNull DdevProjectsPanel panel) {
            super(panel, DdevIntegrationBundle.message("action.DdevIntegration.Run.EditPhpConfig.MainMenu.text"),
                    AllIcons.Actions.EditSource);
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return super.isEnabledFor(selected) && selected.getAppRoot() != null;
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    final Path file = DdevConfigFiles.ensureCustomPhpIni(
                            Path.of(Objects.requireNonNull(selected.getAppRoot())));
                    ApplicationManager.getApplication().invokeLater(() -> this.panel.openLocalFile(file));
                } catch (java.io.IOException ignored) {
                    // The project may have disappeared between list refresh and selection.
                }
            });
        }
    }

    static final class OpenWebserver extends DdevProjectAction {
        OpenWebserver(@NotNull DdevProjectsPanel panel) {
            super(panel,
                    DdevIntegrationBundle.message("action.DdevIntegration.Run.EditWebserverConfig.MainMenu.text"),
                    AllIcons.Actions.EditSource);
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return super.isEnabledFor(selected) && selected.getAppRoot() != null;
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            final Path file = DdevConfigFiles.findWebserverConfig(
                    Path.of(Objects.requireNonNull(selected.getAppRoot())));
            if (file == null) {
                Messages.showInfoMessage(this.panel.project(),
                        DdevIntegrationBundle.message("editWebserverConfig.missing.message"),
                        DdevIntegrationBundle.message("editWebserverConfig.missing.title"));
            } else {
                this.panel.openLocalFile(file);
            }
        }
    }
}
