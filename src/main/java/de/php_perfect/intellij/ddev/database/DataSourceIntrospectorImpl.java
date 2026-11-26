package de.php_perfect.intellij.ddev.database;

import com.intellij.database.dataSource.LocalDataSource;
import com.intellij.database.dataSource.SyncQueue;
import com.intellij.database.util.DataSourceUtilKt;
import com.intellij.database.util.LoaderContext;
import com.intellij.openapi.project.Project;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineStart;
import org.jetbrains.annotations.NotNull;

public final class DataSourceIntrospectorImpl implements DataSourceIntrospector {
    private final @NotNull Project project;
    private final @NotNull CoroutineScope coroutineScope;

    public DataSourceIntrospectorImpl(final @NotNull Project project, final @NotNull CoroutineScope coroutineScope) {
        this.project = project;
        this.coroutineScope = coroutineScope;
    }

    // The platform skips it while Auto sync is off.
    @Override
    @SuppressWarnings("unchecked")
    public void introspect(final @NotNull LocalDataSource dataSource) {
        BuildersKt.launch(this.coroutineScope, EmptyCoroutineContext.INSTANCE, CoroutineStart.DEFAULT, (scope, continuation) ->
                DataSourceUtilKt.performAutoIntrospection(
                        LoaderContext.selectGeneralTask(this.project, dataSource),
                        true,
                        (Continuation<? super SyncQueue.SyncResult>) (Continuation<?>) continuation
                )
        );
    }
}
