package de.php_perfect.intellij.ddev.php.server;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.PathMappingSettings;
import com.jetbrains.php.config.servers.PhpServer;
import com.jetbrains.php.config.servers.PhpServersWorkspaceStateComponent;
import de.php_perfect.intellij.ddev.index.IndexEntry;
import de.php_perfect.intellij.ddev.index.ManagedConfigurationIndex;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public final class ServerConfigManagerImpl implements ServerConfigManager {
    private static final @NotNull String LEGACY_SERVER_NAME = "DDEV";
    private static final @NotNull Logger LOG = Logger.getInstance(ServerConfigManagerImpl.class);

    private final @NotNull Project project;

    public ServerConfigManagerImpl(final @NotNull Project project) {
        this.project = project;
    }

    public void configure(final @NotNull ServerConfig serverConfig) {
        final int hash = serverConfig.hashCode();
        final String fqdn = serverConfig.uri().getHost();

        if (fqdn == null || fqdn.isBlank()) {
            // Happens when DDEV_PRIMARY_URL has no hostname, e.g. with a disabled router (https://github.com/ddev/ddev/issues/7221).
            // A PhpServer without a name/host would be invalid, so leave any existing configuration untouched.
            LOG.warn(String.format("Skipping PHP server configuration because the primary URL %s has no hostname", serverConfig.uri()));
            return;
        }

        final ManagedConfigurationIndex managedConfigurationIndex = ManagedConfigurationIndex.getInstance(this.project);
        final IndexEntry indexEntry = managedConfigurationIndex.get(ServerConfig.class);
        final List<PhpServer> servers = PhpServersWorkspaceStateComponent.getInstance(project).getServers();
        PhpServer phpServer = null;

        if (indexEntry != null && (phpServer = servers.stream()
                .filter(currentPhpServer -> currentPhpServer.getId().equals(indexEntry.id()))
                .findFirst()
                .orElse(null)) != null && indexEntry.hashEquals(hash)) {
            LOG.debug(String.format("server configuration %s is up to date", fqdn));
            return;
        }

        LOG.debug(String.format("Updating server configuration %s", fqdn));

        if (phpServer == null) {
            phpServer = servers.stream()
                    .filter(currentPhpServer -> Objects.equals(currentPhpServer.getName(), LEGACY_SERVER_NAME))
                    .findFirst()
                    .orElse(null);
        }

        if (phpServer == null) {
            phpServer = new PhpServer();
            servers.add(phpServer);
        }

        phpServer.setName(fqdn);
        phpServer.setHost(fqdn);
        phpServer.setPort(80);
        phpServer.setUsePathMappings(true);

        final List<PathMappingSettings.PathMapping> mappings = phpServer.getMappings();
        final PathMappingSettings.PathMapping mapping = new PathMappingSettings.PathMapping(serverConfig.localPath(), serverConfig.remotePathPath());
        mappings.clear();
        mappings.add(mapping);

        managedConfigurationIndex.set(phpServer.getId(), ServerConfig.class, hash);
    }
}
