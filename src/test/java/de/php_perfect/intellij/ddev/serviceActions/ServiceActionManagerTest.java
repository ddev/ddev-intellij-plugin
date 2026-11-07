package de.php_perfect.intellij.ddev.serviceActions;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.php_perfect.intellij.ddev.actions.OpenServiceAction;
import de.php_perfect.intellij.ddev.cmd.Description;
import de.php_perfect.intellij.ddev.cmd.Service;
import de.php_perfect.intellij.ddev.service_actions.ServiceActionChangedListener;
import de.php_perfect.intellij.ddev.service_actions.ServiceActionManager;
import de.php_perfect.intellij.ddev.service_actions.ServiceActionManagerImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class ServiceActionManagerTest extends BasePlatformTestCase {
    private static final String ACTION_ID = "DdevIntegration.Services.ddev-test-web";

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        try {
            final ActionManager actionManager = ActionManager.getInstance();
            if (actionManager.getAction(ACTION_ID) != null) {
                actionManager.unregisterAction(ACTION_ID);
            }
        } finally {
            super.tearDown();
        }
    }

    @Test
    void projectServiceIsServiceActionManagerImpl() {
        Assertions.assertInstanceOf(ServiceActionManagerImpl.class, ServiceActionManager.getInstance(getProject()));
    }

    @Test
    void newManagerHasNoActions() {
        Assertions.assertEquals(0, new ServiceActionManagerImpl().getServiceActions().length);
    }

    @Test
    void listenerForwardsDescriptionToManager() {
        final ServiceActionManager serviceActionManager = Mockito.mock(ServiceActionManager.class);
        ServiceContainerUtil.replaceService(getProject(), ServiceActionManager.class, serviceActionManager, getTestRootDisposable());
        final Description description = Description.builder().name("test").build();

        new ServiceActionChangedListener(getProject()).onDescriptionChanged(description);

        Mockito.verify(serviceActionManager).updateActionsByDescription(description);
    }

    @Test
    void knownServicesGetDedicatedTexts() {
        final ServiceActionManagerImpl serviceActionManager = new ServiceActionManagerImpl();

        serviceActionManager.updateActionsByDescription(Description.builder()
                .services(Map.of(
                        "web", new Service("ddev-test-web", "https://test.ddev.site", null),
                        "mailhog", new Service("ddev-test-mailhog", "https://test.ddev.site:8026", null),
                        "mailpit", new Service("ddev-test-mailpit", "https://test.ddev.site:8027", null),
                        "adminer", new Service("ddev-test-adminer", "https://test.ddev.site:9101", null)
                ))
                .build());

        final Set<String> texts = Arrays.stream(serviceActionManager.getServiceActions())
                .map(action -> action.getTemplatePresentation().getText())
                .collect(Collectors.toSet());

        Assertions.assertEquals(Set.of("Open Website", "Open Mailhog", "Open Mailpit", "Open ddev-test-adminer"), texts);
    }

    @Test
    void disposeUnregistersServiceActions() {
        final AnAction action = new OpenServiceAction(URI.create("https://test.ddev.site"), "Open Website", null, null);
        ActionManager.getInstance().registerAction(ACTION_ID, action);
        final ServiceActionManagerImpl serviceActionManager = new ServiceActionManagerImpl(new HashMap<>(Map.of(ACTION_ID, action)));

        serviceActionManager.dispose();

        Assertions.assertNull(ActionManager.getInstance().getAction(ACTION_ID));
    }
}
