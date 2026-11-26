package de.php_perfect.intellij.ddev.database;

import com.intellij.database.dataSource.LocalDataSource;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public interface DataSourceIntrospector {
    void introspect(final @NotNull LocalDataSource dataSource);

    static DataSourceIntrospector getInstance(final @NotNull Project project) {
        return project.getService(DataSourceIntrospector.class);
    }
}
