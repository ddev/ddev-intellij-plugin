package de.php_perfect.intellij.ddev.state;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

final class ProjectLocksTest {
    private static final long LIVE_PID = ProcessHandle.current().pid();

    @TempDir
    private Path locks;

    @Test
    void theProjectStaysInUseWhileAnotherWindowHoldsIt() {
        final ProjectLocks projectLocks = new ProjectLocks(this.locks);
        projectLocks.acquire("/work/site", "window-a", LIVE_PID);
        projectLocks.acquire("/work/site", "window-b", LIVE_PID);

        assertThat(projectLocks.releaseAndCheckInUse("/work/site", "window-a", LIVE_PID)).isTrue();
        assertThat(projectLocks.releaseAndCheckInUse("/work/site", "window-b", LIVE_PID)).isFalse();
    }

    @Test
    void locksOfEndedProcessesAndOtherProjectsAreIgnored() {
        final ProjectLocks projectLocks = new ProjectLocks(this.locks);
        projectLocks.acquire("/work/site", "crashed", Long.MAX_VALUE);
        projectLocks.acquire("/work/other", "window-b", LIVE_PID);
        projectLocks.acquire("/work/site", "window-a", LIVE_PID);

        assertThat(projectLocks.releaseAndCheckInUse("/work/site", "window-a", LIVE_PID)).isFalse();
    }
}
