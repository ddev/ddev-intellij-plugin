package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.ServiceContainerUtil;
import de.php_perfect.intellij.ddev.cmd.Description;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mockito.Mockito;

public final class DdevStateStub {
    private DdevStateStub() {
    }

    // A null status means DDEV reported no description.
    public static @NotNull DdevStateManager install(@NotNull Project project, @NotNull Disposable disposable,
                                                    boolean available, boolean configured,
                                                    @Nullable Description.Status status) {
        final State state = Mockito.mock(State.class);
        Mockito.when(state.isAvailable()).thenReturn(available);
        Mockito.when(state.isConfigured()).thenReturn(configured);
        Mockito.when(state.getDdevBinary()).thenReturn("ddev");
        Mockito.when(state.getDescription()).thenReturn(status == null ? null : Description.builder().status(status).build());

        final DdevStateManager manager = Mockito.mock(DdevStateManager.class);
        Mockito.when(manager.getState()).thenReturn(state);

        return install(project, disposable, manager);
    }

    public static @NotNull DdevStateManager install(@NotNull Project project, @NotNull Disposable disposable,
                                                    @NotNull DdevStateManager manager) {
        ServiceContainerUtil.replaceService(project, DdevStateManager.class, manager, disposable);

        return manager;
    }
}
