package de.php_perfect.intellij.ddev.database;

import com.intellij.database.dataSource.LocalDataSource;
import com.intellij.database.dataSource.LocalDataSourceManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.php_perfect.intellij.ddev.index.IndexEntry;
import de.php_perfect.intellij.ddev.index.ManagedConfigurationIndex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.awaitility.Awaitility;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

final class DdevDataSourceManagerImplTest extends BasePlatformTestCase {
    private static final DataSourceConfig CONFIG = new DataSourceConfig("DDEV", "DDEV generated data source", DataSourceConfig.Type.MARIADB, "10.11", "127.0.0.1", 1, "db", "db", "db");

    private DataSourceProvider dataSourceProvider;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        this.removeDataSources();
        // Unconfigured data sources are not synchronised by the database plugin.
        this.dataSourceProvider = Mockito.mock(DataSourceProvider.class);
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), DataSourceProvider.class, this.dataSourceProvider, this.getTestRootDisposable());
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        try {
            this.removeDataSources();
            ManagedConfigurationIndex.getInstance(this.getProject()).remove(DataSourceConfig.class);
        } finally {
            super.tearDown();
        }
    }

    @Test
    void createsManagedDataSource() {
        new DdevDataSourceManagerImpl(this.getProject()).updateDdevDataSource(CONFIG);
        this.awaitIntrospection();

        final List<LocalDataSource> dataSources = LocalDataSourceManager.getInstance(this.getProject()).getDataSources();
        Assertions.assertEquals(1, dataSources.size());

        final LocalDataSource dataSource = dataSources.get(0);
        Mockito.verify(this.dataSourceProvider).updateDataSource(dataSource, CONFIG);

        final IndexEntry indexEntry = ManagedConfigurationIndex.getInstance(this.getProject()).get(DataSourceConfig.class);
        Assertions.assertNotNull(indexEntry);
        Assertions.assertEquals(dataSource.getUniqueId(), indexEntry.id());
        Assertions.assertTrue(indexEntry.hashEquals(CONFIG.hashCode()));
    }

    @Test
    void upToDateDataSourceIsLeftUntouched() {
        final DdevDataSourceManagerImpl manager = new DdevDataSourceManagerImpl(this.getProject());
        manager.updateDdevDataSource(CONFIG);
        this.awaitIntrospection();

        manager.updateDdevDataSource(CONFIG);

        Assertions.assertEquals(1, LocalDataSourceManager.getInstance(this.getProject()).getDataSources().size());
        Mockito.verify(this.dataSourceProvider, Mockito.times(1)).updateDataSource(Mockito.any(), Mockito.any());
    }

    @Test
    void changedConfigurationUpdatesManagedDataSource() {
        final DdevDataSourceManagerImpl manager = new DdevDataSourceManagerImpl(this.getProject());
        manager.updateDdevDataSource(CONFIG);
        this.awaitIntrospection();

        final DataSourceConfig changedConfig = new DataSourceConfig("DDEV", "DDEV generated data source", DataSourceConfig.Type.MARIADB, "10.11", "127.0.0.1", 2, "db", "db", "db");
        manager.updateDdevDataSource(changedConfig);
        this.awaitIntrospection();

        final List<LocalDataSource> dataSources = LocalDataSourceManager.getInstance(this.getProject()).getDataSources();
        Assertions.assertEquals(1, dataSources.size());
        Mockito.verify(this.dataSourceProvider).updateDataSource(dataSources.get(0), changedConfig);
        Assertions.assertTrue(ManagedConfigurationIndex.getInstance(this.getProject()).isUpToDate(DataSourceConfig.class, changedConfig.hashCode()));
    }

    @Test
    void legacyDataSourceIsReused() {
        final LocalDataSourceManager localDataSourceManager = LocalDataSourceManager.getInstance(this.getProject());
        final LocalDataSource legacyDataSource = localDataSourceManager.createEmpty();
        legacyDataSource.setName("DDEV");
        localDataSourceManager.addDataSource(legacyDataSource);

        new DdevDataSourceManagerImpl(this.getProject()).updateDdevDataSource(CONFIG);
        this.awaitIntrospection();

        Assertions.assertEquals(List.of(legacyDataSource), localDataSourceManager.getDataSources());
        Mockito.verify(this.dataSourceProvider).updateDataSource(legacyDataSource, CONFIG);
    }

    private void flushEventQueue() {
        ApplicationManager.getApplication().invokeAndWait(() -> {
        });
    }

    private void awaitIntrospection() {
        this.flushEventQueue();
        Awaitility.await().pollInterval(Duration.ofMillis(20)).atMost(Duration.ofSeconds(5))
                .until(DdevDataSourceManagerImplTest::isIntrospectionRunning);
        Awaitility.await().pollInterval(Duration.ofMillis(20)).atMost(Duration.ofSeconds(30))
                .until(() -> !isIntrospectionRunning());
    }

    private static boolean isIntrospectionRunning() {
        final String managerClass = DdevDataSourceManagerImpl.class.getName();

        return Thread.getAllStackTraces().values().stream()
                .flatMap(Arrays::stream)
                .map(StackTraceElement::getClassName)
                .anyMatch(className -> className.equals(managerClass) || className.startsWith(managerClass + "$"));
    }

    private void removeDataSources() {
        final LocalDataSourceManager localDataSourceManager = LocalDataSourceManager.getInstance(this.getProject());
        for (LocalDataSource dataSource : List.copyOf(localDataSourceManager.getDataSources())) {
            localDataSourceManager.removeDataSource(dataSource);
        }
    }
}
