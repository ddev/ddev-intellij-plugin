package de.php_perfect.intellij.ddev.database;

import com.intellij.database.dataSource.LocalDataSource;
import com.intellij.database.dataSource.LocalDataSourceManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.index.IndexEntry;
import de.php_perfect.intellij.ddev.index.ManagedConfigurationIndex;
import org.jetbrains.annotations.NotNull;

public final class DdevDataSourceManagerImpl implements DdevDataSourceManager {
    private static final @NotNull String LEGACY_DATA_SOURCE_NAME = "DDEV";
    private static final @NotNull Logger LOG = Logger.getInstance(DdevDataSourceManagerImpl.class);
    private final @NotNull Project project;

    public DdevDataSourceManagerImpl(final @NotNull Project project) {
        this.project = project;
    }

    @Override
    public void updateDdevDataSource(final @NotNull DataSourceConfig dataSourceConfig) {
        final int hash = dataSourceConfig.hashCode();
        final ManagedConfigurationIndex managedConfigurationIndex = ManagedConfigurationIndex.getInstance(this.project);
        final IndexEntry indexEntry = managedConfigurationIndex.get(DataSourceConfig.class);

        final LocalDataSourceManager localDataSourceManager = LocalDataSourceManager.getInstance(this.project);
        final var dataSources = localDataSourceManager.getDataSources();

        LocalDataSource localDataSource = null;
        if (indexEntry != null && (localDataSource = dataSources.stream()
                .filter(currentDataSource -> currentDataSource.getUniqueId().equals(indexEntry.id()))
                .findFirst()
                .orElse(null)) != null && indexEntry.hashEquals(hash)) {
            LOG.debug(String.format("Data source configuration %s is up to date", dataSourceConfig.name()));
            return;
        }

        LOG.debug(String.format("Updating data source configuration %s", dataSourceConfig.name()));

        if (localDataSource == null) {
            localDataSource = dataSources.stream()
                    .filter(currentDataSource -> currentDataSource.getName().equals(LEGACY_DATA_SOURCE_NAME))
                    .findFirst()
                    .orElse(null);
        }

        if (localDataSource == null) {
            localDataSource = localDataSourceManager.createEmpty();
            dataSources.add(localDataSource);
        }

        final LocalDataSource dataSource = localDataSource;
        DataSourceProvider.getInstance().updateDataSource(dataSource, dataSourceConfig);

        ApplicationManager.getApplication().invokeLater(() -> {
            localDataSourceManager.fireDataSourceUpdated(dataSource);
        });

        managedConfigurationIndex.set(dataSource.getUniqueId(), DataSourceConfig.class, hash);
    }
}
