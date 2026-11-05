package de.php_perfect.intellij.ddev.cmd;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import de.php_perfect.intellij.ddev.DdevConfigArgumentProvider;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.state.DdevConfigLoader;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

public final class DdevRunnerImpl implements DdevRunner {
    private static final ExtensionPointName<DdevConfigArgumentProvider> CONFIG_ARGUMENT_PROVIDER_EP = ExtensionPointName.create("de.php_perfect.intellij.ddev.ddevConfigArgumentProvider");

    @Override
    public void start(@NotNull Project project) {
        final String title = DdevIntegrationBundle.message("ddev.run.start");
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("start", project), title, () -> this.updateDescription(project));
    }

    @Override
    public void restart(@NotNull Project project) {
        final String title = DdevIntegrationBundle.message("ddev.run.restart");
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("restart", project), title, () -> this.updateDescription(project));
    }

    @Override
    public void stop(@NotNull Project project) {
        final String title = DdevIntegrationBundle.message("ddev.run.stop");
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("stop", project), title, () -> this.updateDescription(project));
    }

    @Override
    public void powerOff(@NotNull Project project) {
        final String title = DdevIntegrationBundle.message("ddev.run.powerOff");
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("poweroff", project), title, () -> this.updateDescription(project));
    }

    @Override
    public void delete(@NotNull Project project) {
        final String title = DdevIntegrationBundle.message("ddev.run.delete");
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("delete", project), title, () -> this.updateDescription(project));
    }

    @Override
    public void share(@NotNull Project project) {
        final String title = DdevIntegrationBundle.message("ddev.run.share");
        final Runner runner = Runner.getInstance(project);
        runner.run(this.createCommandLine("share", project), title);
    }

    @Override
    public void config(@NotNull Project project) {
        final String title = DdevIntegrationBundle.message("ddev.run.config");
        final Runner runner = Runner.getInstance(project);
        runner.run(this.buildConfigCommandLine(project), title, () -> {
            this.updateConfiguration(project);
            this.openConfig(project);
        });
    }

    private void openConfig(@NotNull Project project) {
        VirtualFile ddevConfig = DdevConfigLoader.getInstance(project).load();

        if (ddevConfig != null && ddevConfig.exists()) {
            new OpenFileDescriptor(project, ddevConfig).navigate(true);
        }
    }

    private void updateDescription(Project project) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> DdevStateManager.getInstance(project).updateDescription());
    }

    private void updateConfiguration(Project project) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> DdevStateManager.getInstance(project).updateConfiguration());
    }

    private @NotNull GeneralCommandLine buildConfigCommandLine(@NotNull Project project) {
        final GeneralCommandLine commandLine = this.createCommandLine("config", project)
                .withParameters("--auto");

        for (final DdevConfigArgumentProvider ddevConfigArgumentProvider : CONFIG_ARGUMENT_PROVIDER_EP.getExtensionList()) {
            commandLine.addParameters(ddevConfigArgumentProvider.getAdditionalArguments(project));
        }

        return commandLine;
    }

    private @NotNull GeneralCommandLine createCommandLine(@NotNull String ddevAction, @NotNull Project project) {
        State state = DdevStateManager.getInstance(project).getState();

        return new PtyCommandLine(List.of(Objects.requireNonNull(state.getDdevBinary()), ddevAction))
                .withInitialRows(30)
                .withInitialColumns(120)
                .withWorkDirectory(project.getBasePath())
                .withCharset(StandardCharsets.UTF_8)
                .withEnvironment("DDEV_NONINTERACTIVE", "true");
    }
}
