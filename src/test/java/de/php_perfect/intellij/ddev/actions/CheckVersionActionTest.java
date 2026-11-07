package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.php_perfect.intellij.ddev.version.VersionChecker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class CheckVersionActionTest extends BasePlatformTestCase {
    private VersionChecker versionChecker;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        this.versionChecker = Mockito.mock(VersionChecker.class);
        ServiceContainerUtil.replaceService(getProject(), VersionChecker.class, this.versionChecker, getTestRootDisposable());
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void actionChecksVersionAndConfirmsLatest() {
        final CheckVersionAction action = new CheckVersionAction();

        action.actionPerformed(TestActionEvent.createTestEvent(action, SimpleDataContext.getProjectContext(getProject())));

        Mockito.verify(this.versionChecker).checkDdevVersion(true);
    }

    @Test
    void actionWithoutProjectChecksNothing() {
        final CheckVersionAction action = new CheckVersionAction();

        action.actionPerformed(TestActionEvent.createTestEvent(action, DataContext.EMPTY_CONTEXT));

        Mockito.verifyNoInteractions(this.versionChecker);
    }
}
