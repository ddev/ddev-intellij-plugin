package de.php_perfect.intellij.ddev.terminal;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.EmptyAction;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.impl.InternalDecorator;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.php_perfect.intellij.ddev.tutorial.GotItTutorial;
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.*;
import java.awt.*;

final class TutorialListenerTest extends BasePlatformTestCase {
    private GotItTutorial tutorial;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        this.tutorial = Mockito.mock(GotItTutorial.class);
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), GotItTutorial.class, this.tutorial, getTestRootDisposable());
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void otherToolWindowsShowNoTutorial() {
        final ToolWindow toolWindow = toolWindow("Project", new JPanel());

        new TutorialListener().toolWindowShown(toolWindow);

        Mockito.verifyNoInteractions(this.tutorial);
    }

    @Test
    void terminalOutsideDecoratorShowsNoTutorial() {
        final ToolWindow toolWindow = toolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID, new JPanel());

        new TutorialListener().toolWindowShown(toolWindow);

        Mockito.verifyNoInteractions(this.tutorial);
    }

    @Test
    void terminalShowsTutorialOnNewSessionButton() {
        final AnAction newSessionAction = ActionManager.getInstance().getAction("TerminalNewPredefinedSession");
        Assertions.assertNotNull(newSessionAction);

        final ActionButton otherButton = button(new EmptyAction());
        final ActionButton newSessionButton = button(newSessionAction);
        final JPanel content = new JPanel();
        final InternalDecorator decorator = decorator(otherButton, newSessionButton, content);
        Assertions.assertNotNull(decorator);
        final ToolWindow toolWindow = toolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID, content);

        new TutorialListener().toolWindowShown(toolWindow);

        Mockito.verify(this.tutorial).showTerminalTutorial(newSessionButton, toolWindow.getDisposable());
    }

    @Test
    void terminalWithoutNewSessionButtonShowsNoTutorial() {
        final JPanel content = new JPanel();
        decorator(button(new EmptyAction()), content);
        final ToolWindow toolWindow = toolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID, content);

        new TutorialListener().toolWindowShown(toolWindow);

        Mockito.verifyNoInteractions(this.tutorial);
    }

    private ToolWindow toolWindow(String id, JComponent component) {
        final ToolWindow toolWindow = Mockito.mock(ToolWindow.class);
        Mockito.when(toolWindow.getId()).thenReturn(id);
        Mockito.when(toolWindow.getComponent()).thenReturn(component);
        Mockito.when(toolWindow.getDisposable()).thenReturn(getTestRootDisposable());
        return toolWindow;
    }

    private static InternalDecorator decorator(Component... children) {
        final InternalDecorator decorator = Mockito.mock(InternalDecorator.class,
                Mockito.withSettings().useConstructor().defaultAnswer(Mockito.CALLS_REAL_METHODS));
        for (Component child : children) {
            decorator.add(child);
        }
        return decorator;
    }

    private static ActionButton button(AnAction action) {
        return new ActionButton(action, new Presentation(), ActionPlaces.TOOLWINDOW_TITLE, new Dimension(16, 16));
    }
}
