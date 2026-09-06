package de.php_perfect.intellij.ddev.toolwindow;

import de.php_perfect.intellij.ddev.util.DdevProjectRoot;
import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.actions.RevealFileAction;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.messages.MessageBusConnection;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.StateChangedListener;
import de.php_perfect.intellij.ddev.StateInitializedListener;
import de.php_perfect.intellij.ddev.actions.AddOnChooser;
import de.php_perfect.intellij.ddev.actions.ShareProviderChooser;
import de.php_perfect.intellij.ddev.dbmanager.DatabaseOpener;
import de.php_perfect.intellij.ddev.docker.DockerProviderStarter;
import de.php_perfect.intellij.ddev.install.Installers;
import de.php_perfect.intellij.ddev.settings.DdevSettingsConfigurable;
import com.intellij.openapi.options.ShowSettingsUtil;
import de.php_perfect.intellij.ddev.actions.SnapshotChooser;
import de.php_perfect.intellij.ddev.cmd.CommandFailedException;
import de.php_perfect.intellij.ddev.cmd.Ddev;
import de.php_perfect.intellij.ddev.cmd.DdevConfigOptions;
import de.php_perfect.intellij.ddev.cmd.DdevConfigOptionsLoader;
import de.php_perfect.intellij.ddev.cmd.DdevConfigFiles;
import de.php_perfect.intellij.ddev.cmd.DatabaseInfo;
import de.php_perfect.intellij.ddev.cmd.DdevProject;
import de.php_perfect.intellij.ddev.cmd.DdevRunner;
import de.php_perfect.intellij.ddev.cmd.Description;
import de.php_perfect.intellij.ddev.cmd.ShareManager;
import de.php_perfect.intellij.ddev.icons.DdevIntegrationIcons;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import de.php_perfect.intellij.ddev.settings.DdevSettingsState;
import de.php_perfect.intellij.ddev.util.DdevProjectOpener;
import de.php_perfect.intellij.ddev.terminal.DdevTerminalService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static de.php_perfect.intellij.ddev.toolwindow.DdevProjectsTree.*;

public final class DdevProjectsPanel extends SimpleToolWindowPanel {
    private final transient @NotNull Project ideProject;
    private final @NotNull DefaultMutableTreeNode root = new DefaultMutableTreeNode();
    private final @NotNull DefaultTreeModel treeModel = new DefaultTreeModel(this.root);
    private final @NotNull Tree tree;
    private boolean showCurrentProjectOnly = false;
    private transient @NotNull List<DdevProject> lastLoadedProjects = List.of();

    public DdevProjectsPanel(@NotNull Project ideProject, @NotNull Disposable parentDisposable) {
        super(true, true);
        this.ideProject = ideProject;
        this.showCurrentProjectOnly = DdevSettingsState.getInstance(ideProject).showCurrentProjectOnly;

        this.tree = new Tree(this.treeModel);
        this.tree.setRootVisible(false);
        this.tree.setShowsRootHandles(true);
        this.tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        this.tree.setCellRenderer(new DdevProjectsTree.Renderer(ideProject));
        this.tree.getEmptyText().setText(DdevIntegrationBundle.message("toolWindow.projects.empty"));

        this.tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    DdevProjectsPanel.this.openSelectedUrl();
                }
            }
        });

        this.tree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) {
                final Object node = event.getPath().getLastPathComponent();

                if (node instanceof DefaultMutableTreeNode treeNode && treeNode.getUserObject() instanceof ServicesGroup servicesGroup) {
                    DdevProjectsPanel.this.loadServices(treeNode, servicesGroup);
                } else if (node instanceof DefaultMutableTreeNode treeNode
                        && treeNode.getUserObject() instanceof DdevProject
                        && DdevSettingsState.getInstance(DdevProjectsPanel.this.ideProject).expandServicesInProjectsToolWindow) {
                    for (int i = 0; i < treeNode.getChildCount(); i++) {
                        final DefaultMutableTreeNode child = (DefaultMutableTreeNode) treeNode.getChildAt(i);

                        if (child.getUserObject() instanceof ServicesGroup) {
                            ApplicationManager.getApplication().invokeLater(() ->
                                    DdevProjectsPanel.this.tree.expandPath(new TreePath(child.getPath())));
                            break;
                        }
                    }
                }
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) {
                // Nothing to do on collapse.
            }
        });

        final DefaultActionGroup toolbarGroup = new DefaultActionGroup(
                new DdevLifecycleActions.Refresh(this),
                Objects.requireNonNull(ActionManager.getInstance().getAction("DdevIntegration.Run.AddProject")),
                new DdevLifecycleActions.ToggleCurrentProjectOnly(this),
                Separator.getInstance(),
                new DdevLifecycleActions.Start(this),
                new DdevLifecycleActions.Stop(this),
                new DdevLifecycleActions.Restart(this),
                Separator.getInstance(),
                new OpenBrowserAction(),
                Objects.requireNonNull(ActionManager.getInstance().getAction("DdevIntegration.Run.PowerOff"))
        );

        final ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("DdevProjectsToolWindow", toolbarGroup, true);
        toolbar.setTargetComponent(this.tree);
        this.setToolbar(toolbar.getComponent());
        this.setContent(ScrollPaneFactory.createScrollPane(this.tree));

        final DefaultActionGroup contextMenu = new DefaultActionGroup(
                new DdevLifecycleActions.Start(this),
                new DdevLifecycleActions.Stop(this),
                new DdevLifecycleActions.Restart(this),
                new DdevLifecycleActions.StopOthers(this),
                Separator.getInstance(),
                new CreateSnapshotAction(),
                new RestoreSnapshotAction(),
                new DeleteSelectedSnapshotAction(),
                new ClearSelectedSnapshotsAction(),
                Separator.getInstance(),
                new DdevConfigurationActions.Change(this,
                        DdevIntegrationBundle.message("action.DdevIntegration.Run.ChangePhpVersion.MainMenu.text"),
                        DdevIntegrationBundle.message("changePhpVersion.popupTitle"),
                        "--php-version=", DdevConfigOptions::phpVersions),
                new DdevConfigurationActions.ChangeNodejs(this),
                new DdevConfigurationActions.Change(this,
                        DdevIntegrationBundle.message("action.DdevIntegration.Run.ChangeWebserverType.MainMenu.text"),
                        DdevIntegrationBundle.message("changeWebserverType.popupTitle"),
                        "--webserver-type=", DdevConfigOptions::webserverTypes),
                new DdevConfigurationActions.ChangeDatabase(this),
                new DdevConfigurationActions.OpenPhp(this),
                new DdevConfigurationActions.OpenWebserver(this),
                Separator.getInstance(),
                new InstallSelectedAddOnAction(),
                new RemoveSelectedAddOnAction(),
                Separator.getInstance(),
                new EnableSelectedXdebugAction(),
                new DisableSelectedXdebugAction(),
                new ResetSelectedMutagenAction(),
                Separator.getInstance(),
                new ShareSelectedProjectAction(),
                new ShareSelectedProjectWithAction(),
                new StopSharingSelectedProjectAction(),
                Separator.getInstance(),
                new ImportDatabaseAction(),
                new ExportDatabaseAction(),
                new OpenSelectedDatabaseAction(false),
                new OpenSelectedDatabaseAction(true),
                Separator.getInstance(),
                new OpenBrowserAction(),
                new OpenSelectedTerminalAction(),
                new OpenSelectedDatabaseTerminalAction(),
                new OpenInCurrentWindowAction(),
                new OpenInNewWindowAction(),
                new RevealDirectoryAction(),
                Separator.getInstance(),
                new RenameAction(),
                new OpenConfigFileAction(),
                Separator.getInstance(),
                new DeleteProjectAction()
        );
        PopupHandler.installPopupMenu(this.tree, contextMenu, "DdevProjectsPopup");

        // The DDEV binary and project state resolve asynchronously, possibly after this panel exists.
        final MessageBusConnection connection = ideProject.getMessageBus().connect(parentDisposable);
        connection.subscribe(StateInitializedListener.STATE_INITIALIZED, state -> this.refreshLater());
        connection.subscribe(StateChangedListener.DDEV_CHANGED, state -> this.refreshLater());

        this.refresh();
    }

    public void refresh() {
        final State state = DdevStateManager.getInstance(this.ideProject).getState();
        final String binary = state.getDdevBinary();

        if (binary == null) {
            this.tree.getEmptyText().setText(DdevIntegrationBundle.message("toolWindow.projects.notAvailable"));
            this.tree.getEmptyText().appendLine(DdevIntegrationBundle.message("toolWindow.projects.installDdev"),
                    SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES, event -> Installers.installDdev(this.ideProject));
            this.tree.getEmptyText().appendLine(DdevIntegrationBundle.message("toolWindow.projects.configurePath"),
                    SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES, event -> ShowSettingsUtil.getInstance()
                            .showSettingsDialog(this.ideProject, DdevSettingsConfigurable.class));
            this.root.removeAllChildren();
            this.treeModel.reload();
            return;
        }

        new Task.Backgroundable(this.ideProject, DdevIntegrationBundle.message("toolWindow.projects.loading"), true) {
            private @Nullable List<DdevProject> projects;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    this.projects = Ddev.getInstance().listProjects(binary, DdevProjectsPanel.this.ideProject);
                } catch (CommandFailedException exception) {
                    this.projects = null;
                }
            }

            @Override
            public void onSuccess() {
                if (this.projects != null) {
                    DdevProjectsPanel.this.rebuildTree(this.projects);
                } else {
                    DdevProjectsPanel.this.tree.getEmptyText().setText(DdevIntegrationBundle.message("toolWindow.projects.loadFailed"));
                    DdevProjectsPanel.this.tree.getEmptyText().appendLine(
                            DdevIntegrationBundle.message("action.DdevIntegration.StartDockerProvider.text"),
                            SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES,
                            event -> DockerProviderStarter.start(DdevProjectsPanel.this.ideProject));
                    DdevProjectsPanel.this.root.removeAllChildren();
                    DdevProjectsPanel.this.treeModel.reload();
                }
            }
        }.queue();
    }

    private void rebuildTree(@NotNull List<DdevProject> projects) {
        final List<List<String>> expandedKeys = new ArrayList<>();
        final Enumeration<TreePath> expandedPaths = this.tree.getExpandedDescendants(new TreePath(this.root));
        if (expandedPaths != null) {
            while (expandedPaths.hasMoreElements()) {
                expandedKeys.add(nodeKeys(expandedPaths.nextElement()));
            }
        }
        final TreePath selectionPath = this.tree.getSelectionPath();
        final List<String> selectedKeys = selectionPath == null ? null : nodeKeys(selectionPath);

        this.lastLoadedProjects = projects;
        this.tree.getEmptyText().setText(DdevIntegrationBundle.message("toolWindow.projects.empty"));
        this.root.removeAllChildren();

        final String basePath = DdevProjectRoot.of(this.ideProject);

        for (final DdevProject ddevProject : projects) {
            if (this.showCurrentProjectOnly && basePath != null && !FileUtil.pathsEqual(basePath, ddevProject.getAppRoot())) {
                continue;
            }

            final DefaultMutableTreeNode projectNode = new DefaultMutableTreeNode(ddevProject);

            if (ddevProject.getPrimaryUrl() != null && ddevProject.isRunning()) {
                projectNode.add(new DefaultMutableTreeNode(new UrlItem(ddevProject.getPrimaryUrl())));
            }

            if (ddevProject.getShortRoot() != null) {
                projectNode.add(new DefaultMutableTreeNode(new PathItem(ddevProject.getShortRoot(), ddevProject.getAppRoot())));
            }

            if (ddevProject.isRunning() && ddevProject.getName() != null) {
                final DefaultMutableTreeNode servicesNode = new DefaultMutableTreeNode(new ServicesGroup(ddevProject.getName()));
                servicesNode.add(new DefaultMutableTreeNode(new LoadingItem()));
                projectNode.add(servicesNode);
            }

            this.root.add(projectNode);
        }

        this.treeModel.reload();
        this.restoreViewState(expandedKeys, selectedKeys);
    }

    /**
     * Re-expands and re-selects the nodes that match the recorded keys, so reloading the projects
     * keeps what the user has open. Expanding a services group loads its services again.
     */
    private void restoreViewState(@NotNull List<List<String>> expandedKeys, @Nullable List<String> selectedKeys) {
        for (List<String> keys : expandedKeys) {
            final TreePath path = this.findPath(keys);
            if (path.getPathCount() == keys.size() + 1) {
                this.tree.expandPath(path);
            }
        }
        if (selectedKeys != null && !selectedKeys.isEmpty()) {
            final TreePath path = this.findPath(selectedKeys);
            if (path.getPathCount() > 1) {
                this.tree.setSelectionPath(path);
            }
        }
    }

    /**
     * Returns the path of the deepest node matching a prefix of {@code keys}.
     */
    private @NotNull TreePath findPath(@NotNull List<String> keys) {
        DefaultMutableTreeNode node = this.root;
        TreePath path = new TreePath(this.root);
        for (String key : keys) {
            if (key == null) {
                break;
            }
            DefaultMutableTreeNode match = null;
            for (int i = 0; i < node.getChildCount() && match == null; i++) {
                final DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
                if (key.equals(nodeKey(child.getUserObject()))) {
                    match = child;
                }
            }
            if (match == null) {
                break;
            }
            node = match;
            path = path.pathByAddingChild(match);
        }
        return path;
    }

    private static @NotNull List<String> nodeKeys(@NotNull TreePath path) {
        final List<String> keys = new ArrayList<>();
        for (int i = 1; i < path.getPathCount(); i++) {
            keys.add(nodeKey(((DefaultMutableTreeNode) path.getPathComponent(i)).getUserObject()));
        }
        return keys;
    }

    private static @Nullable String nodeKey(@Nullable Object userObject) {
        if (userObject instanceof DdevProject ddevProject) {
            return "project:" + ddevProject.getAppRoot() + ":" + ddevProject.getName();
        } else if (userObject instanceof UrlItem) {
            return "url";
        } else if (userObject instanceof PathItem) {
            return "path";
        } else if (userObject instanceof ServicesGroup) {
            return "services";
        } else if (userObject instanceof ServiceItem serviceItem) {
            return "service:" + serviceItem.name;
        }
        return null;
    }

    private void loadServices(@NotNull DefaultMutableTreeNode servicesNode, @NotNull ServicesGroup servicesGroup) {
        if (servicesGroup.loaded) {
            return;
        }

        servicesGroup.loaded = true;

        final State state = DdevStateManager.getInstance(this.ideProject).getState();
        final String binary = state.getDdevBinary();

        if (binary == null) {
            return;
        }

        new Task.Backgroundable(this.ideProject, DdevIntegrationBundle.message("toolWindow.projects.loadingServices", servicesGroup.projectName), true) {
            private @Nullable Description description;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    this.description = Ddev.getInstance().describeProject(binary, DdevProjectsPanel.this.ideProject, servicesGroup.projectName);
                } catch (CommandFailedException exception) {
                    this.description = null;
                }
            }

            @Override
            public void onSuccess() {
                servicesNode.removeAllChildren();

                if (this.description != null) {
                    this.description.getServices().entrySet().stream()
                            .filter(entry -> !"web".equals(entry.getKey()) && !"db".equals(entry.getKey()))
                            .sorted(Map.Entry.comparingByKey())
                            .forEach(entry -> servicesNode.add(
                                    new DefaultMutableTreeNode(new ServiceItem(entry.getKey(), entry.getValue()))));
                }

                if (this.description == null) {
                    // Left unloaded so the next expansion retries.
                    servicesGroup.loaded = false;
                    servicesNode.add(new DefaultMutableTreeNode(new MessageItem(
                            DdevIntegrationBundle.message("toolWindow.projects.node.servicesFailed"), true)));
                } else if (servicesNode.getChildCount() == 0) {
                    servicesNode.add(new DefaultMutableTreeNode(new MessageItem(
                            DdevIntegrationBundle.message("toolWindow.projects.node.noServices"), false)));
                }

                DdevProjectsPanel.this.treeModel.reload(servicesNode);
                DdevProjectsPanel.this.tree.expandPath(new TreePath(servicesNode.getPath()));
            }
        }.queue();
    }

    void refreshLater() {
        ApplicationManager.getApplication().invokeLater(this::refresh);
    }

    void openLocalFile(@NotNull Path path) {
        final VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path);

        if (file != null) {
            com.intellij.openapi.fileEditor.FileEditorManager.getInstance(this.ideProject).openFile(file, true);
        }
    }

    private @Nullable DefaultMutableTreeNode getSelectedNode() {
        final TreePath selectionPath = this.tree.getSelectionPath();

        if (selectionPath == null) {
            return null;
        }

        return (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
    }

    @Nullable DdevProject getSelectedProject() {
        final DefaultMutableTreeNode node = this.getSelectedNode();
        return node != null && node.getUserObject() instanceof DdevProject ddevProject ? ddevProject : null;
    }

    private @Nullable DdevProject getOwningProject() {
        DefaultMutableTreeNode node = this.getSelectedNode();

        while (node != null) {
            if (node.getUserObject() instanceof DdevProject ddevProject) {
                return ddevProject;
            }

            node = (DefaultMutableTreeNode) node.getParent();
        }

        return null;
    }

    private void openSelectedUrl() {
        final DefaultMutableTreeNode node = this.getSelectedNode();

        if (node == null) {
            return;
        }

        final Object userObject = node.getUserObject();

        if (userObject instanceof UrlItem urlItem) {
            BrowserUtil.browse(urlItem.url);
        } else if (userObject instanceof ServiceItem serviceItem && serviceItem.getUrl() != null) {
            BrowserUtil.browse(serviceItem.getUrl());
        } else if (userObject instanceof DdevProject ddevProject && ddevProject.isRunning() && ddevProject.getPrimaryUrl() != null) {
            BrowserUtil.browse(ddevProject.getPrimaryUrl());
        }
    }

    private @Nullable String getBinary() {
        return DdevStateManager.getInstance(this.ideProject).getState().getDdevBinary();
    }

    @NotNull Project project() {
        return this.ideProject;
    }

    boolean isShowingCurrentProjectOnly() {
        return this.showCurrentProjectOnly;
    }

    void setShowingCurrentProjectOnly(boolean state) {
        this.showCurrentProjectOnly = state;
        DdevSettingsState.getInstance(this.ideProject).showCurrentProjectOnly = state;
        this.rebuildTree(this.lastLoadedProjects);
    }

    // --- Actions ---

    private abstract class SelectionAwareAction extends DdevProjectAction {
        SelectionAwareAction(@NotNull String text, @Nullable Icon icon) {
            super(DdevProjectsPanel.this, text, icon);
        }
    }

    @NotNull List<String> collectOtherRunning(@NotNull DdevProject selected) {
        final java.util.ArrayList<String> result = new java.util.ArrayList<>();

        for (int i = 0; i < this.root.getChildCount(); i++) {
            final Object userObject = ((DefaultMutableTreeNode) this.root.getChildAt(i)).getUserObject();

            if (userObject instanceof DdevProject other && other.isRunning() && other.getName() != null
                    && !Objects.equals(other.getName(), selected.getName())) {
                result.add(other.getName());
            }
        }

        return result;
    }

    private final class InstallSelectedAddOnAction extends SelectionAwareAction {
        InstallSelectedAddOnAction() {
            super(DdevIntegrationBundle.message("action.DdevIntegration.Run.InstallAddOn.MainMenu.text"),
                    AllIcons.Actions.Install);
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return super.isEnabledFor(selected) && selected.getAppRoot() != null;
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            final String binary = DdevProjectsPanel.this.getBinary();

            if (binary != null) {
                AddOnChooser.install(DdevProjectsPanel.this.ideProject, binary, selected.getAppRoot(),
                        DdevProjectsPanel.this::refreshLater);
            }
        }
    }

    private final class RemoveSelectedAddOnAction extends SelectionAwareAction {
        RemoveSelectedAddOnAction() {
            super(DdevIntegrationBundle.message("action.DdevIntegration.Run.RemoveAddOn.MainMenu.text"),
                    AllIcons.Actions.Uninstall);
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return super.isEnabledFor(selected) && selected.getAppRoot() != null;
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            final String binary = DdevProjectsPanel.this.getBinary();

            if (binary != null) {
                AddOnChooser.remove(DdevProjectsPanel.this.ideProject, binary, selected.getAppRoot(),
                        DdevProjectsPanel.this::refreshLater);
            }
        }
    }

    private final class EnableSelectedXdebugAction extends SelectionAwareAction {
        EnableSelectedXdebugAction() {
            super(DdevIntegrationBundle.message("action.DdevIntegration.Run.EnableXdebug.MainMenu.text"),
                    AllIcons.Actions.StartDebugger);
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return super.isEnabledFor(selected) && selected.isRunning() && selected.getAppRoot() != null;
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            DdevRunner.getInstance().enableXdebug(DdevProjectsPanel.this.ideProject, selected.getAppRoot(),
                    DdevProjectsPanel.this::refreshLater);
        }
    }

    private final class DisableSelectedXdebugAction extends SelectionAwareAction {
        DisableSelectedXdebugAction() {
            super(DdevIntegrationBundle.message("action.DdevIntegration.Run.DisableXdebug.MainMenu.text"),
                    AllIcons.Debugger.MuteBreakpoints);
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return super.isEnabledFor(selected) && selected.isRunning() && selected.getAppRoot() != null;
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            DdevRunner.getInstance().disableXdebug(DdevProjectsPanel.this.ideProject, selected.getAppRoot(),
                    DdevProjectsPanel.this::refreshLater);
        }
    }

    private final class ResetSelectedMutagenAction extends SelectionAwareAction {
        ResetSelectedMutagenAction() {
            super(DdevIntegrationBundle.message("action.DdevIntegration.Run.MutagenReset.MainMenu.text"),
                    AllIcons.Actions.ForceRefresh);
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return super.isEnabledFor(selected) && selected.getAppRoot() != null;
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            DdevRunner.getInstance().mutagenReset(DdevProjectsPanel.this.ideProject, selected.getAppRoot(),
                    DdevProjectsPanel.this::refreshLater);
        }
    }

    private final class ShareSelectedProjectAction extends SelectionAwareAction {
        ShareSelectedProjectAction() {
            super(DdevIntegrationBundle.message("action.DdevIntegration.Run.Share.MainMenu.text"), AllIcons.Actions.Share);
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return super.isEnabledFor(selected) && selected.isRunning() && selected.getAppRoot() != null
                    && !ShareManager.getInstance(DdevProjectsPanel.this.ideProject).isSharing();
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            DdevRunner.getInstance().share(DdevProjectsPanel.this.ideProject, selected.getAppRoot(), selected.getDocroot(), null);
        }
    }

    private final class ShareSelectedProjectWithAction extends SelectionAwareAction {
        ShareSelectedProjectWithAction() {
            super(DdevIntegrationBundle.message("action.DdevIntegration.Run.ShareWith.MainMenu.text"), AllIcons.Actions.Share);
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return super.isEnabledFor(selected) && selected.isRunning() && selected.getAppRoot() != null
                    && !ShareManager.getInstance(DdevProjectsPanel.this.ideProject).isSharing();
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            ShareProviderChooser.share(DdevProjectsPanel.this.ideProject, selected.getAppRoot(), selected.getDocroot());
        }
    }

    private final class StopSharingSelectedProjectAction extends SelectionAwareAction {
        StopSharingSelectedProjectAction() {
            super(DdevIntegrationBundle.message("action.DdevIntegration.Run.ShareStop.MainMenu.text"),
                    AllIcons.Actions.Cancel);
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return super.isEnabledFor(selected) && selected.getAppRoot() != null
                    && ShareManager.getInstance(DdevProjectsPanel.this.ideProject).isSharing(selected.getAppRoot());
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            DdevRunner.getInstance().stopShare(DdevProjectsPanel.this.ideProject);
        }
    }

    private final class CreateSnapshotAction extends SelectionAwareAction {
        CreateSnapshotAction() {
            super(DdevIntegrationBundle.message("toolWindow.projects.action.createSnapshot"), AllIcons.Actions.MenuSaveall);
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return super.isEnabledFor(selected) && selected.isRunning();
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            DdevRunner.getInstance().createSnapshot(DdevProjectsPanel.this.ideProject, selected.getAppRoot());
        }
    }

    private final class RestoreSnapshotAction extends SelectionAwareAction {
        RestoreSnapshotAction() {
            super(DdevIntegrationBundle.message("toolWindow.projects.action.restoreSnapshot"), AllIcons.Actions.Rollback);
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return super.isEnabledFor(selected) && selected.isRunning() && selected.getAppRoot() != null;
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            final String binary = DdevProjectsPanel.this.getBinary();

            if (binary != null) {
                SnapshotChooser.restore(DdevProjectsPanel.this.ideProject, binary, selected.getAppRoot());
            }
        }
    }

    private final class DeleteSelectedSnapshotAction extends SelectionAwareAction {
        DeleteSelectedSnapshotAction() {
            super(DdevIntegrationBundle.message("action.DdevIntegration.Run.DeleteSnapshot.MainMenu.text"),
                    AllIcons.General.Delete);
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return super.isEnabledFor(selected) && selected.getAppRoot() != null;
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            final String binary = DdevProjectsPanel.this.getBinary();

            if (binary != null) {
                SnapshotChooser.delete(DdevProjectsPanel.this.ideProject, binary, selected.getAppRoot());
            }
        }
    }

    private final class ClearSelectedSnapshotsAction extends SelectionAwareAction {
        ClearSelectedSnapshotsAction() {
            super(DdevIntegrationBundle.message("action.DdevIntegration.Run.ClearSnapshots.MainMenu.text"),
                    AllIcons.Actions.GC);
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return super.isEnabledFor(selected) && selected.getAppRoot() != null;
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            final boolean confirmed = MessageDialogBuilder.yesNo(
                    DdevIntegrationBundle.message("dialog.clearSnapshots.title"),
                    DdevIntegrationBundle.message("dialog.clearSnapshots.message")
            ).ask(DdevProjectsPanel.this.ideProject);

            if (confirmed) {
                DdevRunner.getInstance().clearSnapshots(
                        DdevProjectsPanel.this.ideProject, selected.getAppRoot(), DdevProjectsPanel.this::refreshLater);
            }
        }
    }

    private final class ImportDatabaseAction extends SelectionAwareAction {
        ImportDatabaseAction() {
            super(DdevIntegrationBundle.message("toolWindow.projects.action.importDatabase"), AllIcons.ToolbarDecorator.Import);
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return super.isEnabledFor(selected) && selected.isRunning() && selected.getAppRoot() != null;
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.singleFile()
                    .withTitle(DdevIntegrationBundle.message("dialog.importDatabase.title"))
                    .withDescription(DdevIntegrationBundle.message("dialog.importDatabase.description"));

            final VirtualFile file = FileChooser.chooseFile(descriptor, DdevProjectsPanel.this.ideProject, null);

            if (file != null) {
                DdevRunner.getInstance().importDatabase(DdevProjectsPanel.this.ideProject,
                        selected.getAppRoot(), file.getPath(), selected.getName(), selected.getType());
            }
        }
    }

    private final class OpenSelectedDatabaseAction extends SelectionAwareAction {
        private final boolean choose;

        OpenSelectedDatabaseAction(boolean choose) {
            super(DdevIntegrationBundle.message(choose
                    ? "action.DdevIntegration.Run.OpenDatabaseWith.MainMenu.text"
                    : "action.DdevIntegration.Run.OpenDatabase.MainMenu.text"), AllIcons.Nodes.DataTables);
            this.choose = choose;
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return super.isEnabledFor(selected) && selected.isRunning() && selected.getAppRoot() != null
                    && selected.getName() != null;
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            if (this.choose) {
                DatabaseOpener.chooseAndOpen(DdevProjectsPanel.this.ideProject, selected.getName(), selected.getAppRoot());
            } else {
                DatabaseOpener.open(DdevProjectsPanel.this.ideProject, selected.getName(), selected.getAppRoot());
            }
        }
    }

    private final class ExportDatabaseAction extends SelectionAwareAction {
        ExportDatabaseAction() {
            super(DdevIntegrationBundle.message("toolWindow.projects.action.exportDatabase"), AllIcons.ToolbarDecorator.Export);
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return super.isEnabledFor(selected) && selected.isRunning() && selected.getAppRoot() != null;
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            final FileSaverDescriptor descriptor = new FileSaverDescriptor(
                    DdevIntegrationBundle.message("dialog.exportDatabase.title"),
                    DdevIntegrationBundle.message("dialog.exportDatabase.description")
            );

            final VirtualFileWrapper fileWrapper = FileChooserFactory.getInstance()
                    .createSaveFileDialog(descriptor, DdevProjectsPanel.this.ideProject)
                    .save((Path) null, selected.getName() + ".sql.gz");

            if (fileWrapper != null) {
                DdevRunner.getInstance().exportDatabase(DdevProjectsPanel.this.ideProject, selected.getAppRoot(), fileWrapper.getFile().getAbsolutePath());
            }
        }
    }

    private final class OpenBrowserAction extends DumbAwareAction {
        OpenBrowserAction() {
            super(DdevIntegrationBundle.message("toolWindow.projects.action.openBrowser"), null, AllIcons.General.Web);
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            final DefaultMutableTreeNode node = DdevProjectsPanel.this.getSelectedNode();
            final Object selected = node != null ? node.getUserObject() : null;
            final String url = getUrl(selected);

            e.getPresentation().setEnabledAndVisible(url != null);

            if (selected instanceof ServiceItem serviceItem) {
                e.getPresentation().setText(DdevIntegrationBundle.message(
                        "toolWindow.projects.action.openService", serviceItem.name));
            } else {
                e.getPresentation().setText(DdevIntegrationBundle.message("toolWindow.projects.action.openBrowser"));
            }
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            DdevProjectsPanel.this.openSelectedUrl();
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }

        private @Nullable String getUrl(@Nullable Object selected) {
            if (selected instanceof UrlItem urlItem) {
                return urlItem.url;
            }

            if (selected instanceof ServiceItem serviceItem) {
                return serviceItem.getUrl();
            }

            return selected instanceof DdevProject ddevProject && ddevProject.isRunning()
                    ? ddevProject.getPrimaryUrl()
                    : null;
        }
    }

    private final class OpenSelectedTerminalAction extends DumbAwareAction {
        OpenSelectedTerminalAction() {
            super(DdevIntegrationBundle.message("toolWindow.projects.action.openTerminal"), null,
                    DdevIntegrationIcons.DdevLogoColor);
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            final DefaultMutableTreeNode node = DdevProjectsPanel.this.getSelectedNode();
            final Object selected = node != null ? node.getUserObject() : null;
            final DdevProject project = DdevProjectsPanel.this.getOwningProject();
            final boolean service = selected instanceof ServiceItem serviceItem
                    && !"mailpit".equals(serviceItem.name) && !"mailhog".equals(serviceItem.name);
            final boolean available = project != null && project.isRunning() && project.getAppRoot() != null
                    && DdevProjectsPanel.this.getTerminalService() != null
                    && (selected instanceof DdevProject || service);
            e.getPresentation().setEnabledAndVisible(available);

            if (service) {
                e.getPresentation().setText(DdevIntegrationBundle.message(
                        "toolWindow.projects.action.openServiceTerminal", ((ServiceItem) selected).name));
            } else {
                e.getPresentation().setText(DdevIntegrationBundle.message("toolWindow.projects.action.openTerminal"));
            }
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            final DefaultMutableTreeNode node = DdevProjectsPanel.this.getSelectedNode();
            final Object selected = node != null ? node.getUserObject() : null;
            final DdevProject project = DdevProjectsPanel.this.getOwningProject();

            if (project == null || project.getAppRoot() == null) {
                return;
            }

            final List<String> arguments;
            final String title;

            if (selected instanceof ServiceItem serviceItem) {
                arguments = List.of("ssh", "--service", serviceItem.name);
                title = "DDEV " + serviceItem.name;
            } else {
                arguments = List.of("ssh");
                title = "DDEV " + project.getName();
            }

            final DdevTerminalService terminalService = DdevProjectsPanel.this.getTerminalService();
            if (terminalService != null) {
                terminalService.open(arguments, title, project.getAppRoot());
            }
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }
    }

    private final class OpenSelectedDatabaseTerminalAction extends SelectionAwareAction {
        OpenSelectedDatabaseTerminalAction() {
            super(DdevIntegrationBundle.message("toolWindow.projects.action.openDatabaseTerminal"),
                    DdevIntegrationIcons.DdevLogoColor);
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return super.isEnabledFor(selected) && selected.isRunning() && selected.getAppRoot() != null
                    && DdevProjectsPanel.this.getTerminalService() != null;
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            final String binary = DdevProjectsPanel.this.getBinary();

            if (binary == null) {
                return;
            }

            new Task.Backgroundable(DdevProjectsPanel.this.ideProject,
                    DdevIntegrationBundle.message("toolWindow.projects.loadingServices",
                            Objects.requireNonNull(selected.getName())), true) {
                private DatabaseInfo databaseInfo;
                private boolean failed;

                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    try {
                        this.databaseInfo = Ddev.getInstance().describeProject(binary,
                                DdevProjectsPanel.this.ideProject, Objects.requireNonNull(selected.getName()))
                                .getDatabaseInfo();
                    } catch (CommandFailedException exception) {
                        this.failed = true;
                    }
                }

                @Override
                public void onSuccess() {
                    if (this.failed) {
                        Messages.showErrorDialog(DdevProjectsPanel.this.ideProject,
                                DdevIntegrationBundle.message("toolWindow.projects.loadFailed"),
                                DdevIntegrationBundle.message("toolWindow.projects.action.openDatabaseTerminal"));
                        return;
                    }

                    if (this.databaseInfo == null || selected.getAppRoot() == null) {
                        return;
                    }

                    final String client = this.databaseInfo.type() == DatabaseInfo.Type.POSTGRESQL ? "psql" : "mysql";
                    final String title = "DDEV Database - " + selected.getName();
                    final DdevTerminalService terminalService = DdevProjectsPanel.this.getTerminalService();
                    if (terminalService != null) {
                        terminalService.open(List.of(client), title, selected.getAppRoot());
                    }
                }
            }.queue();
        }
    }

    private @Nullable DdevTerminalService getTerminalService() {
        return this.ideProject.getService(DdevTerminalService.class);
    }


    private final class OpenInCurrentWindowAction extends SelectionAwareAction {
        OpenInCurrentWindowAction() {
            super(DdevIntegrationBundle.message("toolWindow.projects.action.openInCurrentWindow"), AllIcons.Actions.MenuOpen);
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return selected != null && selected.getAppRoot() != null;
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            if (selected.getAppRoot() != null) {
                DdevProjectOpener.openInCurrentWindow(selected.getAppRoot());
            }
        }
    }

    private final class OpenInNewWindowAction extends SelectionAwareAction {
        OpenInNewWindowAction() {
            super(DdevIntegrationBundle.message("toolWindow.projects.action.openInNewWindow"), AllIcons.Actions.MenuOpen);
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return selected != null && selected.getAppRoot() != null;
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            if (selected.getAppRoot() != null) {
                DdevProjectOpener.openInNewWindow(selected.getAppRoot());
            }
        }
    }

    private final class RevealDirectoryAction extends SelectionAwareAction {
        RevealDirectoryAction() {
            super(DdevIntegrationBundle.message("toolWindow.projects.action.revealDirectory"), AllIcons.Nodes.Folder);
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return selected != null && selected.getAppRoot() != null;
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            if (selected.getAppRoot() != null) {
                RevealFileAction.openDirectory(new File(selected.getAppRoot()));
            }
        }
    }

    private final class RenameAction extends SelectionAwareAction {
        RenameAction() {
            super(DdevIntegrationBundle.message("toolWindow.projects.action.rename"), null);
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return super.isEnabledFor(selected) && selected.getAppRoot() != null;
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            final String newName = Messages.showInputDialog(
                    DdevProjectsPanel.this.ideProject,
                    DdevIntegrationBundle.message("renameProject.message"),
                    DdevIntegrationBundle.message("renameProject.title"),
                    null,
                    selected.getName(),
                    null
            );

            if (newName != null && !newName.isBlank() && !newName.equals(selected.getName())) {
                DdevRunner.getInstance().renameProject(DdevProjectsPanel.this.ideProject, selected.getAppRoot(),
                        Objects.requireNonNull(selected.getName()), newName.trim(),
                        DdevProjectsPanel.this::refreshLater);
            }
        }
    }

    private final class OpenConfigFileAction extends SelectionAwareAction {
        OpenConfigFileAction() {
            super(DdevIntegrationBundle.message("toolWindow.projects.action.openConfig"), AllIcons.Actions.EditSource);
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return selected != null && selected.getAppRoot() != null;
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            final String appRoot = selected.getAppRoot();

            if (appRoot == null) {
                return;
            }

            final Path configPath = Path.of(appRoot, ".ddev", "config.yaml");
            final VirtualFile configFile = com.intellij.openapi.vfs.LocalFileSystem.getInstance().refreshAndFindFileByNioFile(configPath);

            if (configFile != null) {
                com.intellij.openapi.fileEditor.FileEditorManager.getInstance(DdevProjectsPanel.this.ideProject).openFile(configFile, true);
            }
        }
    }

    private final class DeleteProjectAction extends SelectionAwareAction {
        DeleteProjectAction() {
            super(DdevIntegrationBundle.message("toolWindow.projects.action.delete"), AllIcons.General.Delete);
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            final boolean confirmed = MessageDialogBuilder.yesNo(
                    DdevIntegrationBundle.message("toolWindow.projects.delete.confirm.title"),
                    DdevIntegrationBundle.message("toolWindow.projects.delete.confirm.message", selected.getName())
            ).ask(DdevProjectsPanel.this.ideProject);

            if (confirmed) {
                DdevRunner.getInstance().deleteProject(DdevProjectsPanel.this.ideProject,
                        Objects.requireNonNull(selected.getName()), selected.getAppRoot(), DdevProjectsPanel.this::refreshLater);
            }
        }
    }
}
