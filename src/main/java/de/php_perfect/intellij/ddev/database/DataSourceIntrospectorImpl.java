package de.php_perfect.intellij.ddev.database;

import com.intellij.database.dataSource.LocalDataSource;
import com.intellij.database.util.DataSourceUtilKt;
import com.intellij.database.util.LoaderContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import kotlin.coroutines.AbstractCoroutineContextElement;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineExceptionHandler;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineStart;
import org.jetbrains.annotations.NotNull;

public final class DataSourceIntrospectorImpl implements DataSourceIntrospector {
    private static final @NotNull Logger LOG = Logger.getInstance(DataSourceIntrospectorImpl.class);
    private final @NotNull Project project;
    private final @NotNull CoroutineScope coroutineScope;

    public DataSourceIntrospectorImpl(final @NotNull Project project, final @NotNull CoroutineScope coroutineScope) {
        this.project = project;
        this.coroutineScope = coroutineScope;
    }

    // The platform skips it while Auto sync is off.
    @Override
    public void introspect(final @NotNull LocalDataSource dataSource) {
        BuildersKt.launch(this.coroutineScope, new LoggingExceptionHandler(), CoroutineStart.DEFAULT, (scope, continuation) ->
                DataSourceUtilKt.performAutoIntrospection(
                        LoaderContext.selectGeneralTask(this.project, dataSource),
                        true,
                        withResultType(continuation)
                )
        );
    }

    // launch discards the result, so its type is irrelevant.
    @SuppressWarnings("unchecked")
    private static <T> @NotNull Continuation<T> withResultType(final @NotNull Continuation<?> continuation) {
        return (Continuation<T>) continuation;
    }

    // Database plugin failures stay out of the IDE error reporter.
    private static final class LoggingExceptionHandler extends AbstractCoroutineContextElement implements CoroutineExceptionHandler {
        LoggingExceptionHandler() {
            super(CoroutineExceptionHandler.Key);
        }

        @Override
        public void handleException(final @NotNull CoroutineContext context, final @NotNull Throwable exception) {
            LOG.warn("Data source introspection failed", exception);
        }
    }
}
