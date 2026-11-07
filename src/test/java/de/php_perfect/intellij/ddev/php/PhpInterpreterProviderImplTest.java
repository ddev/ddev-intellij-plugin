package de.php_perfect.intellij.ddev.php;

import com.intellij.remote.RemoteMappingsManager;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.jetbrains.php.composer.ComposerDataService;
import com.jetbrains.php.composer.execution.ComposerExecution;
import com.jetbrains.php.config.PhpProjectConfigurationFacade;
import com.jetbrains.php.config.interpreters.PhpInterpreter;
import com.jetbrains.php.config.interpreters.PhpInterpretersManagerImpl;
import com.jetbrains.php.config.interpreters.PhpInterpretersPhpInfoCacheImpl;
import com.jetbrains.php.config.phpInfo.PhpInfo;
import com.jetbrains.php.config.phpInfo.PhpInfoUtil;
import com.jetbrains.php.remote.interpreter.PhpRemoteSdkAdditionalData;
import de.php_perfect.intellij.ddev.index.IndexEntry;
import de.php_perfect.intellij.ddev.index.ManagedConfigurationIndex;
import de.php_perfect.intellij.ddev.php.composer.DdevComposerExecution;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

final class PhpInterpreterProviderImplTest extends BasePlatformTestCase {
    private static final String COMPOSE_FILE = "/project/.ddev/.ddev-docker-compose-full.yaml";

    private final PhpInfo phpInfo = new PhpInfo();
    private ComposerExecution originalComposerExecution;
    private MockedStatic<PhpInfoUtil> phpInfoUtil;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        // Running the interpreter would contact the local Docker daemon.
        this.phpInfoUtil = Mockito.mockStatic(PhpInfoUtil.class);
        this.phpInfoUtil.when(() -> PhpInfoUtil.getPhpInfo(Mockito.any(), Mockito.any(PhpInterpreter.class), Mockito.any())).thenReturn(this.phpInfo);
        this.originalComposerExecution = ComposerDataService.getInstance(this.getProject()).getComposerExecution();
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        try {
            this.phpInfoUtil.close();
            PhpInterpretersManagerImpl.getInstance(this.getProject()).setInterpreters(new ArrayList<>());
            PhpProjectConfigurationFacade.getInstance(this.getProject()).getProjectConfiguration().setInterpreterName(null);
            ComposerDataService.getInstance(this.getProject()).setComposerExecution(this.originalComposerExecution);
            ManagedConfigurationIndex.getInstance(this.getProject()).remove(DdevInterpreterConfig.class);
        } finally {
            super.tearDown();
        }
    }

    @Test
    void registersNewInterpreter() {
        final DdevInterpreterConfig config = new DdevInterpreterConfig("acol", "php8.1", COMPOSE_FILE);

        new PhpInterpreterProviderImpl(this.getProject()).registerInterpreter(config);

        final List<PhpInterpreter> interpreters = PhpInterpretersManagerImpl.getInstance(this.getProject()).getInterpreters();
        Assertions.assertEquals(1, interpreters.size());

        final PhpInterpreter interpreter = interpreters.get(0);
        Assertions.assertEquals("acol", interpreter.getName());
        Assertions.assertTrue(interpreter.isProjectLevel());
        Assertions.assertInstanceOf(PhpRemoteSdkAdditionalData.class, interpreter.getPhpSdkAdditionalData());
        Assertions.assertEquals("acol", PhpProjectConfigurationFacade.getInstance(this.getProject()).getProjectConfiguration().getInterpreterName());
        Assertions.assertInstanceOf(DdevComposerExecution.class, ComposerDataService.getInstance(this.getProject()).getComposerExecution());
        Assertions.assertSame(this.phpInfo, PhpInterpretersPhpInfoCacheImpl.getInstance(this.getProject()).getPhpInfo("acol"));

        final IndexEntry indexEntry = ManagedConfigurationIndex.getInstance(this.getProject()).get(DdevInterpreterConfig.class);
        Assertions.assertNotNull(indexEntry);
        Assertions.assertEquals(interpreter.getId(), indexEntry.id());
        Assertions.assertTrue(indexEntry.hashEquals(config.hashCode()));

        final RemoteMappingsManager.Mappings mappings = RemoteMappingsManager.getInstance(this.getProject()).getForServer("php", interpreter.getId());
        Assertions.assertNotNull(mappings);
        Assertions.assertEquals("/var/www/html", mappings.getSettings().get(0).getRemoteRoot());
    }

    @Test
    void upToDateInterpreterIsLeftUntouched() {
        final DdevInterpreterConfig config = new DdevInterpreterConfig("acol", "php8.1", COMPOSE_FILE);
        final PhpInterpreterProviderImpl provider = new PhpInterpreterProviderImpl(this.getProject());
        provider.registerInterpreter(config);

        final PhpInterpreter interpreter = PhpInterpretersManagerImpl.getInstance(this.getProject()).getInterpreters().get(0);
        interpreter.setName("renamed");

        provider.registerInterpreter(config);

        final List<PhpInterpreter> interpreters = PhpInterpretersManagerImpl.getInstance(this.getProject()).getInterpreters();
        Assertions.assertEquals(1, interpreters.size());
        Assertions.assertEquals("renamed", interpreters.get(0).getName());
    }

    @Test
    void changedConfigurationUpdatesManagedInterpreter() {
        final PhpInterpreterProviderImpl provider = new PhpInterpreterProviderImpl(this.getProject());
        provider.registerInterpreter(new DdevInterpreterConfig("acol", "php8.1", COMPOSE_FILE));
        final String id = PhpInterpretersManagerImpl.getInstance(this.getProject()).getInterpreters().get(0).getId();

        final DdevInterpreterConfig changedConfig = new DdevInterpreterConfig("acol", "php8.3", COMPOSE_FILE);
        provider.registerInterpreter(changedConfig);

        final List<PhpInterpreter> interpreters = PhpInterpretersManagerImpl.getInstance(this.getProject()).getInterpreters();
        Assertions.assertEquals(1, interpreters.size());
        Assertions.assertEquals(id, interpreters.get(0).getId());
        Assertions.assertTrue(ManagedConfigurationIndex.getInstance(this.getProject()).isUpToDate(DdevInterpreterConfig.class, changedConfig.hashCode()));
    }

    @Test
    void legacyInterpreterIsReused() {
        final PhpInterpretersManagerImpl interpretersManager = PhpInterpretersManagerImpl.getInstance(this.getProject());
        final PhpInterpreter legacyInterpreter = new PhpInterpreter();
        legacyInterpreter.setName("DDEV");
        legacyInterpreter.setIsProjectLevel(true);
        interpretersManager.setInterpreters(new ArrayList<>(List.of(legacyInterpreter)));

        PhpProjectConfigurationFacade.getInstance(this.getProject()).getProjectConfiguration().setInterpreterName("other");
        final DdevComposerExecution composerExecution = new DdevComposerExecution();
        ComposerDataService.getInstance(this.getProject()).setComposerExecution(composerExecution);

        new PhpInterpreterProviderImpl(this.getProject()).registerInterpreter(new DdevInterpreterConfig("acol", "php8.1", COMPOSE_FILE));

        final List<PhpInterpreter> interpreters = interpretersManager.getInterpreters();
        Assertions.assertEquals(1, interpreters.size());
        Assertions.assertEquals(legacyInterpreter.getId(), interpreters.get(0).getId());
        Assertions.assertEquals("acol", interpreters.get(0).getName());
        Assertions.assertEquals("other", PhpProjectConfigurationFacade.getInstance(this.getProject()).getProjectConfiguration().getInterpreterName());
        Assertions.assertSame(composerExecution, ComposerDataService.getInstance(this.getProject()).getComposerExecution());
    }
}
