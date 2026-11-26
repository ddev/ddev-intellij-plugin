package de.php_perfect.intellij.ddev.database;

import com.intellij.database.dataSource.LocalDataSource;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import kotlinx.coroutines.CompletableJob;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.coroutines.JobKt;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

final class DataSourceIntrospectorTest extends BasePlatformTestCase {
    private CompletableJob job;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        this.job = JobKt.Job(null);
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        try {
            this.job.cancel(null);
        } finally {
            super.tearDown();
        }
    }

    @Test
    void introspectionCompletesInServiceScope() {
        final LocalDataSource dataSource = new LocalDataSource();
        dataSource.setAutoSynchronize(false);

        new DataSourceIntrospectorImpl(this.getProject(), CoroutineScopeKt.CoroutineScope(this.job)).introspect(dataSource);

        Assertions.assertTrue(this.job.getChildren().iterator().hasNext());
        Awaitility.await().atMost(Duration.ofSeconds(10)).until(() -> !this.job.getChildren().iterator().hasNext());
        Assertions.assertFalse(this.job.isCancelled());
    }
}
