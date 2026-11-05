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
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

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
    void createsManagedDataSource() throws InterruptedException {
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
    void upToDateDataSourceIsLeftUntouched() throws InterruptedException {
        final DdevDataSourceManagerImpl manager = new DdevDataSourceManagerImpl(this.getProject());
        manager.updateDdevDataSource(CONFIG);
        this.awaitIntrospection();

        manager.updateDdevDataSource(CONFIG);

        Assertions.assertEquals(1, LocalDataSourceManager.getInstance(this.getProject()).getDataSources().size());
        Mockito.verify(this.dataSourceProvider, Mockito.times(1)).updateDataSource(Mockito.any(), Mockito.any());
    }

    @Test
    void changedConfigurationUpdatesManagedDataSource() throws InterruptedException {
        final DdevDataSourceManagerImpl manager = new DdevDataSourceManagerImpl(this.getProject());
        manager.updateDdevDataSource(CONFIG);
        this.awaitIntrospection();

        final DataSourceConfig changedConfig = new DataSourceConfig("DDEV", "DDEV generated data source", DataSourceConfig.Type.MARIADB, "10.11", "127.0.0.1", 2, "db", "db", "db");
        manager.updateDdevDataSource(changedConfig);
        this.flushEventQueue();

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
        this.flushEventQueue();

        Assertions.assertEquals(List.of(legacyDataSource), localDataSourceManager.getDataSources());
        Mockito.verify(this.dataSourceProvider).updateDataSource(legacyDataSource, CONFIG);
    }

    private void flushEventQueue() {
        ApplicationManager.getApplication().invokeAndWait(() -> {
        });
    }

    private void awaitIntrospection() throws InterruptedException {
        this.flushEventQueue();
        waitWhile(() -> !isIntrospectionRunning(), 5);
        waitWhile(DdevDataSourceManagerImplTest::isIntrospectionRunning, 30);
    }

    private static boolean isIntrospectionRunning() {
        final String managerClass = DdevDataSourceManagerImpl.class.getName();

        return Thread.getAllStackTraces().values().stream()
                .flatMap(Arrays::stream)
                .map(StackTraceElement::getClassName)
                .anyMatch(className -> className.equals(managerClass) || className.startsWith(managerClass + "$"));
    }

    private static void waitWhile(BooleanSupplier condition, long seconds) throws InterruptedException {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
        while (condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
    }

    private void removeDataSources() {
        final LocalDataSourceManager localDataSourceManager = LocalDataSourceManager.getInstance(this.getProject());
        for (LocalDataSource dataSource : List.copyOf(localDataSourceManager.getDataSources())) {
            localDataSourceManager.removeDataSource(dataSource);
        }
    }
}
