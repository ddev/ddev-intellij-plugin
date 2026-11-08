package de.php_perfect.intellij.ddev.database;

import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.php_perfect.intellij.ddev.cmd.DatabaseInfo;
import de.php_perfect.intellij.ddev.settings.DdevSettingsState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class AutoConfigureDataSourceListenerTest extends BasePlatformTestCase {
    private DdevDataSourceManager dataSourceManager;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        this.dataSourceManager = Mockito.mock(DdevDataSourceManager.class);
        ServiceContainerUtil.replaceService(this.getProject(), DdevDataSourceManager.class, this.dataSourceManager, this.getTestRootDisposable());
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        DdevSettingsState.getInstance(this.getProject()).autoConfigureDataSource = true;
        super.tearDown();
    }

    @Test
    void configuresMysqlDataSource() {
        this.notifyChanged(info(DatabaseInfo.Type.MYSQL, "8.0", "db", "user", "pass", 32768));

        this.verifyDataSource(DataSourceConfig.Type.MYSQL, "8.0");
    }

    @Test
    void configuresMariaDbDataSource() {
        this.notifyChanged(info(DatabaseInfo.Type.MARIADB, "10.11", "db", "user", "pass", 32768));

        this.verifyDataSource(DataSourceConfig.Type.MARIADB, "10.11");
    }

    @Test
    void configuresPostgresDataSource() {
        this.notifyChanged(info(DatabaseInfo.Type.POSTGRESQL, "16", "db", "user", "pass", 32768));

        this.verifyDataSource(DataSourceConfig.Type.POSTGRESQL, "16");
    }

    @Test
    void missingDatabaseInfoIsIgnored() {
        this.notifyChanged(null);

        Mockito.verifyNoInteractions(this.dataSourceManager);
    }

    @Test
    void disabledSettingIsRespected() {
        DdevSettingsState.getInstance(this.getProject()).autoConfigureDataSource = false;

        this.notifyChanged(info(DatabaseInfo.Type.MYSQL, "8.0", "db", "user", "pass", 32768));

        Mockito.verifyNoInteractions(this.dataSourceManager);
    }

    @Test
    void incompleteDatabaseInfoIsIgnored() {
        this.notifyChanged(info(null, "8.0", "db", "user", "pass", 32768));
        this.notifyChanged(info(DatabaseInfo.Type.MYSQL, null, "db", "user", "pass", 32768));
        this.notifyChanged(info(DatabaseInfo.Type.MYSQL, "8.0", null, "user", "pass", 32768));
        this.notifyChanged(info(DatabaseInfo.Type.MYSQL, "8.0", "db", null, "pass", 32768));
        this.notifyChanged(info(DatabaseInfo.Type.MYSQL, "8.0", "db", "user", null, 32768));

        Mockito.verifyNoInteractions(this.dataSourceManager);
    }

    @Test
    void unpublishedPortIsIgnored() {
        this.notifyChanged(info(DatabaseInfo.Type.MYSQL, "8.0", "db", "user", "pass", 0));

        Mockito.verifyNoInteractions(this.dataSourceManager);
    }

    private void notifyChanged(DatabaseInfo databaseInfo) {
        new AutoConfigureDataSourceListener(this.getProject()).onDatabaseInfoChanged(databaseInfo);
    }

    private void verifyDataSource(DataSourceConfig.Type type, String version) {
        Mockito.verify(this.dataSourceManager).updateDdevDataSource(
                new DataSourceConfig("DDEV", "DDEV generated data source", type, version, "127.0.0.1", 32768, "db", "user", "pass")
        );
    }

    private static DatabaseInfo info(DatabaseInfo.Type type, String version, String name, String username, String password, int publishedPort) {
        return new DatabaseInfo(type, version, 3306, name, "db", username, password, publishedPort);
    }
}
