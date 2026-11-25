package de.php_perfect.intellij.ddev.terminal;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public final class DdevTerminalRunner extends PlatformTerminalRunner {
    public DdevTerminalRunner(@NotNull Project project) {
        super(project);
    }

    @Override
    protected @NotNull List<String> createSessionCommand() throws ExecutionException {
        final State ddevState = DdevStateManager.getInstance(this.myProject).getState();

        if (!ddevState.isAvailable()) {
            throw new ExecutionException("DDEV not installed", null);
        }

        return List.of(Objects.requireNonNull(ddevState.getDdevBinary()), "ssh");
    }

    @Override
    public @NlsContexts.TabTitle String getDefaultTabTitle() {
        return "DDEV Web Container";
    }

    @Override
    public boolean isTerminalSessionPersistent() {
        return false;
    }
}
