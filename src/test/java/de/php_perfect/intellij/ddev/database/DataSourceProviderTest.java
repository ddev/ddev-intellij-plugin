package de.php_perfect.intellij.ddev.database;

import com.intellij.database.Dbms;
import com.intellij.database.dataSource.LocalDataSource;
import com.intellij.database.dataSource.SchemaControl;
import com.intellij.database.introspection.DBIntrospectionOptions;
import com.intellij.database.model.properties.Level;
import com.intellij.database.util.TreePattern;
import com.intellij.database.util.TreePatternUtils;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class DataSourceProviderTest extends BasePlatformTestCase {
    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void mySql() {
        final DataSourceConfig dataSourceConfig = new DataSourceConfig("DDEV", "Some Description", DataSourceConfig.Type.MYSQL, "8.0", "127.0.0.1", 12345, "db", "some-user", "some-password");

        final LocalDataSource dataSource = new LocalDataSource();
        new DataSourceProviderImpl().createDataSource(dataSource, dataSourceConfig);

        Assertions.assertInstanceOf(LocalDataSource.class, dataSource);
        Assertions.assertNotNull(dataSource);
        Assertions.assertNotNull(dataSource.getDatabaseDriver());
        Assertions.assertEquals("mysql.8", dataSource.getDatabaseDriver().getId());
        Assertions.assertEquals("jdbc:mysql://127.0.0.1:12345/db?user=some-user&password=some-password", dataSource.getUrl());
    }

    @Test
    void mySql56() {
        final DataSourceConfig dataSourceConfig = new DataSourceConfig("DDEV", "Some Description", DataSourceConfig.Type.MYSQL, "5.6", "127.0.0.1", 12345, "db", "some-user", "some-password");

        final LocalDataSource dataSource = new LocalDataSource();
        new DataSourceProviderImpl().createDataSource(dataSource, dataSourceConfig);

        Assertions.assertInstanceOf(LocalDataSource.class, dataSource);
        Assertions.assertNotNull(dataSource);
        Assertions.assertNotNull(dataSource.getDatabaseDriver());
        Assertions.assertEquals("mysql.8", dataSource.getDatabaseDriver().getId());
        Assertions.assertEquals("jdbc:mysql://127.0.0.1:12345/db?user=some-user&password=some-password", dataSource.getUrl());
    }

    void mySql51() {
        final DataSourceConfig dataSourceConfig = new DataSourceConfig("DDEV", "Some Description", DataSourceConfig.Type.MYSQL, "5.1", "127.0.0.1", 12345, "db", "some-user", "some-password");

        final LocalDataSource dataSource = new LocalDataSource();
        new DataSourceProviderImpl().createDataSource(dataSource, dataSourceConfig);

        Assertions.assertInstanceOf(LocalDataSource.class, dataSource);
        Assertions.assertNotNull(dataSource);
        Assertions.assertNotNull(dataSource.getDatabaseDriver());
        Assertions.assertEquals("mysql", dataSource.getDatabaseDriver().getId());
        Assertions.assertEquals("jdbc:mysql://127.0.0.1:12345/db?user=some-user&password=some-password", dataSource.getUrl());
    }

    @Test
    void mariaDb() {
        final DataSourceConfig dataSourceConfig = new DataSourceConfig("DDEV", "Some Description", DataSourceConfig.Type.MARIADB, "10.4", "127.0.0.1", 12345, "db", "some-user", "some-password");

        final LocalDataSource dataSource = new LocalDataSource();
        new DataSourceProviderImpl().createDataSource(dataSource, dataSourceConfig);

        Assertions.assertInstanceOf(LocalDataSource.class, dataSource);
        Assertions.assertNotNull(dataSource);
        Assertions.assertEquals("jdbc:mariadb://127.0.0.1:12345/db?user=some-user&password=some-password", dataSource.getUrl());
    }

    @Test
    void postgreSql() {
        final DataSourceConfig dataSourceConfig = new DataSourceConfig("DDEV", "Some Description", DataSourceConfig.Type.POSTGRESQL, "15.5", "127.0.0.1", 12345, "db", "some-user", "some-password");

        final LocalDataSource dataSource = new LocalDataSource();
        new DataSourceProviderImpl().createDataSource(dataSource, dataSourceConfig);

        Assertions.assertInstanceOf(LocalDataSource.class, dataSource);
        Assertions.assertNotNull(dataSource);
        Assertions.assertEquals("jdbc:postgresql://127.0.0.1:12345/db?user=some-user&password=some-password", dataSource.getUrl());
    }

    @Test
    void updateKeepsUserSettings() {
        final DataSourceProviderImpl provider = new DataSourceProviderImpl();
        final LocalDataSource dataSource = new LocalDataSource();
        provider.createDataSource(dataSource, postgreSqlConfig(12345));

        final TreePattern userScope = TreePatternUtils.importPattern(Dbms.forConnection(dataSource), "db:public");
        dataSource.setName("My database");
        dataSource.setComment("My comment");
        dataSource.setAutoSynchronize(false);
        dataSource.setCheckOutdated(false);
        dataSource.setSchemaControl(SchemaControl.MANUAL);
        dataSource.setSourceLoading(DBIntrospectionOptions.SourceLoading.NO_SOURCES);
        dataSource.setIntrospectionScope(userScope);
        dataSource.setIntrospectionLevel(Level.L1);

        provider.updateDataSource(dataSource, postgreSqlConfig(23456));

        Assertions.assertEquals("jdbc:postgresql://127.0.0.1:23456/db?user=some-user&password=some-password", dataSource.getUrl());
        Assertions.assertEquals("My database", dataSource.getName());
        Assertions.assertEquals("My comment", dataSource.getComment());
        Assertions.assertFalse(dataSource.isAutoSynchronize());
        Assertions.assertFalse(dataSource.shouldCheckOutdated());
        Assertions.assertEquals(SchemaControl.MANUAL, dataSource.getSchemaControl());
        Assertions.assertEquals(DBIntrospectionOptions.SourceLoading.NO_SOURCES, dataSource.getSourceLoading());
        Assertions.assertSame(userScope, dataSource.getIntrospectionScope());
        Assertions.assertEquals(Level.L1, dataSource.getIntrospectionLevel());
    }

    @Test
    void updateResetsIntrospectionWhenDbmsChanges() {
        final DataSourceProviderImpl provider = new DataSourceProviderImpl();
        final LocalDataSource dataSource = new LocalDataSource();
        provider.createDataSource(dataSource, postgreSqlConfig(12345));

        final TreePattern userScope = TreePatternUtils.importPattern(Dbms.forConnection(dataSource), "db:public");
        dataSource.setIntrospectionScope(userScope);
        dataSource.setIntrospectionLevel(Level.L1);

        provider.updateDataSource(dataSource, new DataSourceConfig("DDEV", "Some Description", DataSourceConfig.Type.MARIADB, "10.11", "127.0.0.1", 12345, "db", "some-user", "some-password"));

        Assertions.assertEquals("mariadb", dataSource.getDatabaseDriver().getId());
        Assertions.assertNotSame(userScope, dataSource.getIntrospectionScope());
        Assertions.assertNotEquals(Level.L1, dataSource.getIntrospectionLevel());
    }

    private static DataSourceConfig postgreSqlConfig(final int port) {
        return new DataSourceConfig("DDEV", "Some Description", DataSourceConfig.Type.POSTGRESQL, "15.5", "127.0.0.1", port, "db", "some-user", "some-password");
    }
}
