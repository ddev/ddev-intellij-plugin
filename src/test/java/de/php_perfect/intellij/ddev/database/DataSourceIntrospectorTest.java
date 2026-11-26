package de.php_perfect.intellij.ddev.database;

import com.intellij.database.dataSource.LocalDataSource;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import kotlinx.coroutines.CompletableJob;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.coroutines.ExecutorsKt;
import kotlinx.coroutines.JobKt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Queue;

final class DataSourceIntrospectorTest extends BasePlatformTestCase {
    private CompletableJob job;
    private Queue<Runnable> dispatched;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        this.job = JobKt.Job(null);
        this.dispatched = new ArrayDeque<>();
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
    void serviceIsRegistered() {
        Assertions.assertInstanceOf(DataSourceIntrospectorImpl.class, DataSourceIntrospector.getInstance(this.getProject()));
    }

    @Test
    void introspectionCompletesInServiceScope() {
        final LocalDataSource dataSource = new LocalDataSource();
        dataSource.setAutoSynchronize(false);
        final var scope = CoroutineScopeKt.CoroutineScope(this.job.plus(ExecutorsKt.from(this.dispatched::add)));

        new DataSourceIntrospectorImpl(this.getProject(), scope).introspect(dataSource);

        Assertions.assertTrue(this.job.getChildren().iterator().hasNext());
        this.runDispatched();
        Assertions.assertFalse(this.job.getChildren().iterator().hasNext());
        Assertions.assertFalse(this.job.isCancelled());
    }

    private void runDispatched() {
        Runnable task;
        while ((task = this.dispatched.poll()) != null) {
            task.run();
        }
    }
}
