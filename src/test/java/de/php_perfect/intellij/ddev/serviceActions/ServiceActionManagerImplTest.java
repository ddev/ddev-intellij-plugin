package de.php_perfect.intellij.ddev.serviceActions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import de.php_perfect.intellij.ddev.actions.OpenServiceAction;
import de.php_perfect.intellij.ddev.cmd.DatabaseInfo;
import de.php_perfect.intellij.ddev.cmd.DatabaseInfo.Type;
import de.php_perfect.intellij.ddev.cmd.Description;
import de.php_perfect.intellij.ddev.cmd.Description.Status;
import de.php_perfect.intellij.ddev.cmd.Service;
import de.php_perfect.intellij.ddev.service_actions.ServiceActionManagerImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Arrays.array;

final class ServiceActionManagerImplTest {

    @Test
    @DisplayName("should update actions from given description")
    void testUpdateActionsByDescription() {
        var serviceActionManager = new ServiceActionManagerImpl(
                new HashMap<>(Map.of(
                        "existingAction",
                        anAction("Open existing action", "https://www.existing.com", "existing action")
                )));

        serviceActionManager.updateActionsByDescription(aDescription());

        assertThat(serviceActionManager.getServiceActions()).isEqualTo(array(
                aMailhogAction(),
                anAction("Open test", "https://www.test.com", "Open test service in your browser")
        ));
    }

    @Test
    @DisplayName("should update actions from given description")
    void testUpdateActionsByMailpitDescription() {
        var serviceActionManager = new ServiceActionManagerImpl(
                new HashMap<>(Map.of(
                        "existingAction",
                        anAction("Open existing action", "https://www.existing.com", "existing action")
                )));

        serviceActionManager.updateActionsByDescription(aMailpitDescription());

        assertThat(serviceActionManager.getServiceActions()).isEqualTo(array(
                aMailhogAction(),
                anAction("Open test", "https://www.test.com", "Open test service in your browser")
        ));
    }

    private AnAction anAction(String displayText, String uri, String description) {
        return new OpenServiceAction(URI.create(uri), displayText, description, AllIcons.General.Web);
    }

    private AnAction aMailhogAction() {
        return anAction("Open Mailhog", "https://www.test.com",
                "Open Mailhog service in your browser");
    }

    private Description aDescription() {
        var dataBaseInfo = new DatabaseInfo(Type.MYSQL, "5.7", 2133, "db", "localhost",
                "root", "root", 2133);

        var httpUrl = "http://www.test.com";
        var httpsUrl = "https://www.test.com";

        return Description.builder()
                .name("test")
                .phpVersion("7.4")
                .status(Status.RUNNING)
                .mailHogHttpsUrl(httpsUrl)
                .mailHogHttpUrl(httpUrl)
                .services(Map.of("test", new Service("test", httpsUrl, httpUrl)))
                .databaseInfo(dataBaseInfo)
                .build();
    }

    private Description aMailpitDescription() {
        var dataBaseInfo = new DatabaseInfo(Type.MYSQL, "5.7", 2133, "db", "localhost",
                "root", "root", 2133);

        var httpUrl = "http://www.test.com";
        var httpsUrl = "https://www.test.com";

        return Description.builder()
                .name("test")
                .phpVersion("7.4")
                .status(Status.RUNNING)
                .mailpitHttpsUrl(httpsUrl)
                .mailpitHttpUrl(httpUrl)
                .services(Map.of("test", new Service("test", httpsUrl, httpUrl)))
                .databaseInfo(dataBaseInfo)
                .build();
    }
}
