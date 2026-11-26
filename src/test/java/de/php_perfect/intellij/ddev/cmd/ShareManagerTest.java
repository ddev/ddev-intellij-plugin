package de.php_perfect.intellij.ddev.cmd;

import com.intellij.execution.process.ProcessHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class ShareManagerTest {
    @Test
    void sharesWhileTheProcessRuns() {
        final ShareManager shareManager = new ShareManager();
        final ProcessHandler processHandler = Mockito.mock(ProcessHandler.class);

        Assertions.assertFalse(shareManager.isSharing());

        shareManager.setShareProcessHandler(processHandler);
        Assertions.assertTrue(shareManager.isSharing());

        Mockito.when(processHandler.isProcessTerminated()).thenReturn(true);
        Assertions.assertFalse(shareManager.isSharing());
    }

    @Test
    void stopSharingDestroysTheRunningProcess() {
        final ShareManager shareManager = new ShareManager();
        final ProcessHandler processHandler = Mockito.mock(ProcessHandler.class);
        shareManager.setShareProcessHandler(processHandler);

        shareManager.stopSharing();

        Mockito.verify(processHandler).destroyProcess();
        Assertions.assertFalse(shareManager.isSharing());
    }

    @Test
    void stopSharingLeavesATerminatedProcessAlone() {
        final ShareManager shareManager = new ShareManager();
        final ProcessHandler processHandler = Mockito.mock(ProcessHandler.class);
        Mockito.when(processHandler.isProcessTerminated()).thenReturn(true);
        shareManager.setShareProcessHandler(processHandler);

        shareManager.stopSharing();

        Mockito.verify(processHandler, Mockito.never()).destroyProcess();
    }
}
