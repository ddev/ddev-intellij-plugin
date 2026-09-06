package de.php_perfect.intellij.ddev.node;

import com.intellij.docker.remote.DockerComposeCredentialsHolder;
import com.intellij.docker.remote.DockerComposeCredentialsType;
import com.intellij.execution.ExecutionException;
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager;
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef;
import com.intellij.javascript.nodejs.interpreter.local.NodeJsLocalInterpreterType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.PathMappingSettings;
import com.jetbrains.nodejs.remote.NodeJSRemoteInterpreterManager;
import com.jetbrains.nodejs.remote.NodeJSRemoteSdkAdditionalData;
import com.jetbrains.nodejs.remote.NodeRemoteInterpreters;
import de.php_perfect.intellij.ddev.docker_compose.DockerComposeConfig;
import de.php_perfect.intellij.ddev.docker_compose.DockerComposeCredentialProvider;
import de.php_perfect.intellij.ddev.index.IndexEntry;
import de.php_perfect.intellij.ddev.index.ManagedConfigurationIndex;
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
        final ManagedConfigurationIndex index = ManagedConfigurationIndex.getInstance(this.project);
        final IndexEntry entry = index.get(NodeInterpreterConfig.class);
        final DockerComposeCredentialsType type = DockerComposeCredentialsType.getInstance();
        NodeJSRemoteSdkAdditionalData sdkData = nodeRemoteInterpreters.getInterpreters().stream()
                .filter(data -> (entry != null && entry.id().equals(data.getSdkId()))
                        || data.getRemoteConnectionType() == type
                        && "web".equals(data.connectionCredentials().getCredentials(type).getComposeServiceName())
                        && data.connectionCredentials().getCredentials(type).getComposeFilePaths()
                        .equals(List.of(nodeInterpreterConfig.composeFilePath())))
                .findFirst().orElse(null);

        if (sdkData != null && entry != null && entry.id().equals(sdkData.getSdkId())
                && entry.hashEquals(nodeInterpreterConfig.hashCode())) {
            return;
        }

        LOG.debug("Configuring nodejs interpreter for " + nodeInterpreterConfig.name());

        final DockerComposeCredentialsHolder credentials = DockerComposeCredentialProvider.getInstance().getDdevDockerComposeCredentials(new DockerComposeConfig(List.of(nodeInterpreterConfig.composeFilePath()), nodeInterpreterConfig.name()));
        if (sdkData == null) {
            sdkData = this.buildNodeJSRemoteSdkAdditionalData(credentials, nodeInterpreterConfig.binaryPath());
            nodeRemoteInterpreters.add(sdkData);
        } else {
            sdkData.setInterpreterPath(nodeInterpreterConfig.binaryPath());
            sdkData.setCredentials(type.getCredentialsKey(), credentials);
            sdkData.setPathMappings(this.loadPathMappings(sdkData));
        }

        final NodeJsInterpreterManager manager = NodeJsInterpreterManager.getInstance(this.project);
        final NodeJsInterpreterRef current = manager.getInterpreterRef();
        if (current == null || current.isProjectRef() || NodeJsLocalInterpreterType.isNodeFromPathRef(current)
                || entry != null && entry.id().equals(current.getReferenceName())) {
            manager.setInterpreterRef(NodeJsInterpreterRef.create(sdkData.getSdkId()));
        }
        index.set(sdkData.getSdkId(), NodeInterpreterConfig.class, nodeInterpreterConfig.hashCode());
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
