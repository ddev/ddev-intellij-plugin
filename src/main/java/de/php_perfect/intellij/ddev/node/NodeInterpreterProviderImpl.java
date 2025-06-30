package de.php_perfect.intellij.ddev.node;

import com.intellij.docker.remote.DockerComposeCredentialsHolder;
import com.intellij.docker.remote.DockerComposeCredentialsType;
import com.intellij.execution.ExecutionException;
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager;
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.PathMappingSettings;
import com.jetbrains.nodejs.remote.NodeJSRemoteInterpreterManager;
import com.jetbrains.nodejs.remote.NodeJSRemoteSdkAdditionalData;
import com.jetbrains.nodejs.remote.NodeRemoteInterpreters;
import de.php_perfect.intellij.ddev.docker_compose.DockerComposeConfig;
import de.php_perfect.intellij.ddev.docker_compose.DockerComposeCredentialProvider;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class NodeInterpreterProviderImpl implements NodeInterpreterProvider {
    public static final @NotNull String NODEJS_HELPERS_PATH = ".webstorm_nodejs_helpers";
    private static final @NotNull Logger LOG = Logger.getInstance(NodeInterpreterProviderImpl.class);
    private final @NotNull Project project;

    public NodeInterpreterProviderImpl(final @NotNull Project project) {
        this.project = project;
    }

    public void configureNodeInterpreter(final @NotNull NodeInterpreterConfig nodeInterpreterConfig) {
        final NodeRemoteInterpreters nodeRemoteInterpreters = NodeRemoteInterpreters.getInstance();

        if (!nodeRemoteInterpreters.getInterpreters().isEmpty()) {
            return;
        }

        LOG.debug("Creating nodejs interpreter");

        final DockerComposeCredentialsHolder credentials = DockerComposeCredentialProvider.getInstance().getDdevDockerComposeCredentials(new DockerComposeConfig(List.of(nodeInterpreterConfig.composeFilePath()), nodeInterpreterConfig.name()));
        final NodeJSRemoteSdkAdditionalData sdkData = this.buildNodeJSRemoteSdkAdditionalData(credentials, nodeInterpreterConfig.binaryPath());
        nodeRemoteInterpreters.add(sdkData);

        NodeJsInterpreterManager.getInstance(this.project).setInterpreterRef(NodeJsInterpreterRef.create(sdkData.getSdkId()));
    }

    private @NotNull NodeJSRemoteSdkAdditionalData buildNodeJSRemoteSdkAdditionalData(DockerComposeCredentialsHolder credentials, @NotNull String binaryPath) {
        final NodeJSRemoteSdkAdditionalData sdkData = new NodeJSRemoteSdkAdditionalData(binaryPath);
        sdkData.setCredentials(DockerComposeCredentialsType.getInstance().getCredentialsKey(), credentials);
        sdkData.setHelpersPath(NODEJS_HELPERS_PATH);
        sdkData.setPathMappings(this.loadPathMappings(sdkData));

        return sdkData;
    }

    private PathMappingSettings loadPathMappings(NodeJSRemoteSdkAdditionalData sdkData) {
        final NodeJSRemoteInterpreterManager nodeRemoteInterpreterManager = NodeJSRemoteInterpreterManager.getInstance();

        try {
            return nodeRemoteInterpreterManager.setupMappings(this.project, sdkData);
        } catch (ExecutionException e) {
            return null;
        }
    }
}
