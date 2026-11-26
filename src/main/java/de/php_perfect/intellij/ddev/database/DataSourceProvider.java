package de.php_perfect.intellij.ddev.database;

import com.intellij.database.dataSource.LocalDataSource;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

public interface DataSourceProvider {
    void createDataSource(final @NotNull LocalDataSource dataSource, final @NotNull DataSourceConfig dataSourceConfig);

    void updateDataSource(final @NotNull LocalDataSource dataSource, final @NotNull DataSourceConfig dataSourceConfig);

    static DataSourceProvider getInstance() {
        return ApplicationManager.getApplication().getService(DataSourceProvider.class);
    }
}
