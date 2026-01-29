package de.php_perfect.intellij.ddev.php;

import com.intellij.docker.remote.DockerComposeCredentialsHolder;
import com.intellij.docker.remote.DockerComposeCredentialsType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.remote.RemoteMappingsManager;
import com.intellij.util.PathMappingSettings;
import com.jetbrains.php.composer.ComposerDataService;
import com.jetbrains.php.config.PhpProjectConfigurationFacade;
import com.jetbrains.php.config.interpreters.PhpInterpreter;
import com.jetbrains.php.config.interpreters.PhpInterpretersManagerImpl;
import com.jetbrains.php.config.interpreters.PhpInterpretersPhpInfoCacheImpl;
import com.jetbrains.php.config.phpInfo.PhpInfo;
import com.jetbrains.php.config.phpInfo.PhpInfoUtil;
import de.php_perfect.intellij.ddev.php.composer.DdevComposerExecution;
import com.jetbrains.php.remote.docker.compose.PhpDockerComposeStartCommand;
import com.jetbrains.php.remote.docker.compose.PhpDockerComposeTypeData;
import com.jetbrains.php.remote.interpreter.PhpRemoteSdkAdditionalData;
import com.jetbrains.php.run.remote.PhpRemoteInterpreterManager;
import de.php_perfect.intellij.ddev.docker_compose.DockerComposeConfig;
import de.php_perfect.intellij.ddev.docker_compose.DockerComposeCredentialProvider;
import de.php_perfect.intellij.ddev.index.IndexEntry;
import de.php_perfect.intellij.ddev.index.ManagedConfigurationIndex;
import de.php_perfect.intellij.ddev.notification.DdevNotifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class PhpInterpreterProviderImpl implements PhpInterpreterProvider {
    private static final String LEGACY_INTERPRETER_NAME = "DDEV";
    private static final String HELPERS_DIR = "/opt/.phpstorm_helpers";
    private static final @NotNull Logger LOG = Logger.getInstance(PhpInterpreterProviderImpl.class);
    private final @NotNull Project project;

    public PhpInterpreterProviderImpl(final @NotNull Project project) {
        this.project = project;
    }

    public void registerInterpreter(final @NotNull DdevInterpreterConfig interpreterConfig) {
        final int hash = interpreterConfig.hashCode();
        final String name = interpreterConfig.name();
        final PhpInterpretersManagerImpl interpretersManager = PhpInterpretersManagerImpl.getInstance(this.project);
        final ManagedConfigurationIndex managedConfigurationIndex = ManagedConfigurationIndex.getInstance(this.project);
        final IndexEntry indexEntry = managedConfigurationIndex.get(DdevInterpreterConfig.class);

        PhpInterpreter interpreter = null;
        if (indexEntry != null && (interpreter = interpretersManager.findInterpreterById(indexEntry.id())) != null && indexEntry.hashEquals(hash)) {
            LOG.debug(String.format("PHP interpreter configuration %s is up to date", name));
            return;
        }

        LOG.debug(String.format("Updating PHP interpreter configuration %s", name));

        if (interpreter == null) {
            interpreter = interpretersManager.findInterpreter(LEGACY_INTERPRETER_NAME);
        }

        if (interpreter == null) {
            interpreter = new PhpInterpreter();
            interpreter.setIsProjectLevel(true);
            List<PhpInterpreter> interpreters = interpretersManager.getInterpreters();
            interpreters.add(interpreter);
            interpretersManager.setInterpreters(interpreters);
        }

        this.updateInterpreter(interpreter, interpreterConfig);
        this.loadPhpInfo(interpreter);
        this.setDefaultIfNotSet(interpreter);
        this.updateComposerInterpreterIfNotSet();
        this.updateRemoteMapping(interpreter);

        managedConfigurationIndex.set(interpreter.getId(), DdevInterpreterConfig.class, hash);
        DdevNotifier.getInstance(project).notifyPhpInterpreterUpdated(interpreterConfig.phpVersion());
    }

    private void updateRemoteMapping(@NotNull PhpInterpreter interpreter) {
        final var pathMapping = new PathMappingSettings.PathMapping(project.getBasePath(), "/var/www/html");
        final var mappings = new RemoteMappingsManager.Mappings();
        mappings.setServerId("php", interpreter.getId());
        mappings.setSettings(List.of(pathMapping));

        RemoteMappingsManager.getInstance(project).setForServer(mappings);
    }

    private void updateComposerInterpreterIfNotSet() {
        final ComposerDataService composerSettings = ComposerDataService.getInstance(project);

        if (!(composerSettings.getComposerExecution() instanceof DdevComposerExecution)) {
            composerSettings.setComposerExecution(new DdevComposerExecution());
        }
    }

    private void updateInterpreter(@NotNull PhpInterpreter interpreter, @NotNull DdevInterpreterConfig interpreterConfig) {
        final DockerComposeCredentialsHolder credentials = DockerComposeCredentialProvider.getInstance().getDdevDockerComposeCredentials(new DockerComposeConfig(List.of(interpreterConfig.composeFilePath()), interpreterConfig.name()));
        final PhpRemoteSdkAdditionalData sdkData = this.buildSdkAdditionalData(interpreter, interpreterConfig, credentials);
        interpreter.setName(interpreterConfig.name());
        interpreter.setPhpSdkAdditionalData(sdkData);
        interpreter.setHomePath(sdkData.getSdkId());
    }

    private @NotNull PhpRemoteSdkAdditionalData buildSdkAdditionalData(@NotNull PhpInterpreter interpreter, @NotNull DdevInterpreterConfig interpreterConfig, DockerComposeCredentialsHolder credentials) {
        final PhpRemoteSdkAdditionalData sdkData = new PhpRemoteSdkAdditionalData(interpreterConfig.phpVersion());
        sdkData.setInterpreterId(interpreter.getId());
        sdkData.setHelpersPath(HELPERS_DIR);
        sdkData.setTypeData(new PhpDockerComposeTypeData(PhpDockerComposeStartCommand.EXEC));
        sdkData.setCredentials(DockerComposeCredentialsType.getInstance().getCredentialsKey(), credentials);
        sdkData.setPathMappings(this.loadPathMappings(sdkData));

        return sdkData;
    }

    private PathMappingSettings loadPathMappings(PhpRemoteSdkAdditionalData sdkData) {
        final PhpRemoteInterpreterManager phpRemoteInterpreterManager = PhpRemoteInterpreterManager.getInstance();

        if (phpRemoteInterpreterManager != null) {
            return phpRemoteInterpreterManager.createPathMappings(this.project, sdkData);
        }

        return null;
    }

    private void loadPhpInfo(final PhpInterpreter interpreter) {
        final PhpInfo phpInfo = PhpInfoUtil.getPhpInfo(this.project, interpreter, null);
        PhpInterpretersPhpInfoCacheImpl.getInstance(this.project).setPhpInfo(interpreter.getName(), phpInfo);
    }

    private void setDefaultIfNotSet(final PhpInterpreter interpreter) {
        final var phpConfigurationFacade = PhpProjectConfigurationFacade.getInstance(this.project);
        final var phpConfiguration = phpConfigurationFacade.getProjectConfiguration();

        if (phpConfiguration.getInterpreterName() == null) {
            phpConfiguration.setInterpreterName(interpreter.getName());
        }
    }
}
