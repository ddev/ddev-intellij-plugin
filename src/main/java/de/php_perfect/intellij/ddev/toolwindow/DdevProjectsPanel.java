package de.php_perfect.intellij.ddev.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.actions.RevealFileAction;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
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
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.treeStructure.Tree;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.CommandFailedException;
import de.php_perfect.intellij.ddev.cmd.Ddev;
import de.php_perfect.intellij.ddev.cmd.DdevProject;
import de.php_perfect.intellij.ddev.cmd.DdevRunner;
import de.php_perfect.intellij.ddev.cmd.Description;
import de.php_perfect.intellij.ddev.cmd.Service;
import de.php_perfect.intellij.ddev.cmd.Snapshot;
import de.php_perfect.intellij.ddev.icons.DdevIntegrationIcons;
import de.php_perfect.intellij.ddev.notification.DdevNotifier;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import de.php_perfect.intellij.ddev.util.DdevProjectOpener;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DdevProjectsPanel extends SimpleToolWindowPanel {
    private final transient @NotNull Project ideProject;
    private final @NotNull DefaultMutableTreeNode root = new DefaultMutableTreeNode();
    private final @NotNull DefaultTreeModel treeModel = new DefaultTreeModel(this.root);
    private final @NotNull Tree tree;
    private boolean showCurrentProjectOnly = false;
    private transient @NotNull List<DdevProject> lastLoadedProjects = List.of();

    public DdevProjectsPanel(@NotNull Project ideProject) {
        super(true, true);
        this.ideProject = ideProject;

        this.tree = new Tree(this.treeModel);
        this.tree.setRootVisible(false);
        this.tree.setShowsRootHandles(true);
        this.tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        this.tree.setCellRenderer(new ProjectsTreeRenderer());
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
                }
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) {
                // Nothing to do on collapse.
            }
        });

        final DefaultActionGroup toolbarGroup = new DefaultActionGroup(
                new RefreshAction(),
                Objects.requireNonNull(ActionManager.getInstance().getAction("DdevIntegration.Run.AddProject")),
                new ToggleCurrentProjectOnlyAction(),
                Separator.getInstance(),
                new StartAction(),
                new StopAction(),
                new RestartAction(),
                Separator.getInstance(),
                new OpenBrowserAction(),
                Objects.requireNonNull(ActionManager.getInstance().getAction("DdevIntegration.Run.PowerOff"))
        );

        final ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("DdevProjectsToolWindow", toolbarGroup, true);
        toolbar.setTargetComponent(this.tree);
        this.setToolbar(toolbar.getComponent());
        this.setContent(ScrollPaneFactory.createScrollPane(this.tree));

        final DefaultActionGroup contextMenu = new DefaultActionGroup(
                new StartAction(),
                new StopAction(),
                new RestartAction(),
                new StopOthersAction(),
                Separator.getInstance(),
                new CreateSnapshotAction(),
                new RestoreSnapshotAction(),
                Separator.getInstance(),
                new ImportDatabaseAction(),
                new ExportDatabaseAction(),
                Separator.getInstance(),
                new OpenBrowserAction(),
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

        this.refresh();
    }

    public void refresh() {
        final State state = DdevStateManager.getInstance(this.ideProject).getState();
        final String binary = state.getDdevBinary();

        if (binary == null) {
            this.tree.getEmptyText().setText(DdevIntegrationBundle.message("toolWindow.projects.notAvailable"));
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
                    DdevProjectsPanel.this.root.removeAllChildren();
                    DdevProjectsPanel.this.treeModel.reload();
                }
            }
        }.queue();
    }

    private void rebuildTree(@NotNull List<DdevProject> projects) {
        this.lastLoadedProjects = projects;
        this.root.removeAllChildren();

        final String basePath = this.ideProject.getBasePath();

        for (final DdevProject ddevProject : projects) {
            if (this.showCurrentProjectOnly && basePath != null && !basePath.equals(ddevProject.getAppRoot())) {
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
                    for (final Map.Entry<String, Service> entry : this.description.getServices().entrySet()) {
                        servicesNode.add(new DefaultMutableTreeNode(new ServiceItem(entry.getKey(), entry.getValue())));
                    }
                }

                if (servicesNode.getChildCount() == 0) {
                    servicesGroup.loaded = false;
                    servicesNode.add(new DefaultMutableTreeNode(new LoadingItem()));
                }

                DdevProjectsPanel.this.treeModel.reload(servicesNode);
                DdevProjectsPanel.this.tree.expandPath(new TreePath(servicesNode.getPath()));
            }
        }.queue();
    }

    private void refreshLater() {
        ApplicationManager.getApplication().invokeLater(this::refresh);
    }

    private @Nullable DefaultMutableTreeNode getSelectedNode() {
        final TreePath selectionPath = this.tree.getSelectionPath();

        if (selectionPath == null) {
            return null;
        }

        return (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
    }

    private @Nullable DdevProject getSelectedProject() {
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

    // --- Tree item models ---

    private static final class UrlItem {
        private final @NotNull String url;

        private UrlItem(@NotNull String url) {
            this.url = url;
        }
    }

    private static final class PathItem {
        private final @NotNull String displayPath;
        private final @Nullable String fullPath;

        private PathItem(@NotNull String displayPath, @Nullable String fullPath) {
            this.displayPath = displayPath;
            this.fullPath = fullPath;
        }
    }

    private static final class ServicesGroup {
        private final @NotNull String projectName;
        private boolean loaded;

        private ServicesGroup(@NotNull String projectName) {
            this.projectName = projectName;
        }
    }

    private static final class ServiceItem {
        private final @NotNull String name;
        private final @NotNull Service service;

        private ServiceItem(@NotNull String name, @NotNull Service service) {
            this.name = name;
            this.service = service;
        }

        private @Nullable String getUrl() {
            return this.service.getHttpsUrl() != null ? this.service.getHttpsUrl() : this.service.getHttpUrl();
        }
    }

    private static final class LoadingItem {
    }

    private final class ProjectsTreeRenderer extends ColoredTreeCellRenderer {
        @Override
        public void customizeCellRenderer(@NotNull JTree jTree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            final Object userObject = ((DefaultMutableTreeNode) value).getUserObject();

            if (userObject instanceof DdevProject ddevProject) {
                this.setIcon(DdevIntegrationIcons.DdevLogoMono);
                this.append(ddevProject.getName() != null ? ddevProject.getName() : "?", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);

                if (ddevProject.getStatusDesc() != null) {
                    this.append("  " + ddevProject.getStatusDesc(), ddevProject.isRunning()
                            ? new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, com.intellij.ui.JBColor.namedColor("Label.successForeground", new com.intellij.ui.JBColor(0x368746, 0x50A661)))
                            : SimpleTextAttributes.GRAYED_ATTRIBUTES);
                }

                if (ddevProject.getType() != null) {
                    this.append("  [" + ddevProject.getType() + "]", SimpleTextAttributes.GRAYED_ATTRIBUTES);
                }
            } else if (userObject instanceof UrlItem urlItem) {
                this.setIcon(AllIcons.General.Web);
                this.append(urlItem.url, SimpleTextAttributes.LINK_ATTRIBUTES);
            } else if (userObject instanceof PathItem pathItem) {
                this.setIcon(AllIcons.Nodes.Folder);
                this.append(pathItem.displayPath, SimpleTextAttributes.REGULAR_ATTRIBUTES);
            } else if (userObject instanceof ServicesGroup) {
                this.setIcon(AllIcons.Nodes.PpLib);
                this.append(DdevIntegrationBundle.message("toolWindow.projects.node.services"), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            } else if (userObject instanceof ServiceItem serviceItem) {
                this.setIcon(AllIcons.Nodes.Plugin);
                this.append(serviceItem.name, SimpleTextAttributes.REGULAR_ATTRIBUTES);

                if (serviceItem.getUrl() != null) {
                    this.append("  " + serviceItem.getUrl(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
                }
            } else if (userObject instanceof LoadingItem) {
                this.append(DdevIntegrationBundle.message("toolWindow.projects.node.loading"), SimpleTextAttributes.GRAYED_ATTRIBUTES);
            }
        }
    }

    // --- Actions ---

    private abstract class SelectionAwareAction extends DumbAwareAction {
        SelectionAwareAction(@NotNull String text, @Nullable Icon icon) {
            super(text, null, icon);
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setEnabledAndVisible(this.isEnabledFor(DdevProjectsPanel.this.getSelectedProject()));
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }

        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return selected != null && selected.getName() != null;
        }

        @Override
        public final void actionPerformed(@NotNull AnActionEvent e) {
            final DdevProject selected = DdevProjectsPanel.this.getSelectedProject();

            if (selected != null && selected.getName() != null) {
                this.perform(selected);
            }
        }

        protected abstract void perform(@NotNull DdevProject selected);
    }

    private final class ToggleCurrentProjectOnlyAction extends com.intellij.openapi.actionSystem.ToggleAction implements com.intellij.openapi.project.DumbAware {
        ToggleCurrentProjectOnlyAction() {
            super(DdevIntegrationBundle.message("toolWindow.projects.action.toggleCurrentOnly"), null, AllIcons.General.Filter);
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return DdevProjectsPanel.this.showCurrentProjectOnly;
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            DdevProjectsPanel.this.showCurrentProjectOnly = state;
            DdevProjectsPanel.this.rebuildTree(DdevProjectsPanel.this.lastLoadedProjects);
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }
    }

    private final class RefreshAction extends DumbAwareAction {
        RefreshAction() {
            super(DdevIntegrationBundle.message("toolWindow.projects.action.refresh"), null, AllIcons.Actions.Refresh);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            DdevProjectsPanel.this.refresh();
        }
    }

    private final class StartAction extends SelectionAwareAction {
        StartAction() {
            super(DdevIntegrationBundle.message("toolWindow.projects.action.start"), AllIcons.Actions.Execute);
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return super.isEnabledFor(selected) && !selected.isRunning();
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            DdevRunner.getInstance().startProject(DdevProjectsPanel.this.ideProject, Objects.requireNonNull(selected.getName()), DdevProjectsPanel.this::refreshLater);
        }
    }

    private final class StopAction extends SelectionAwareAction {
        StopAction() {
            super(DdevIntegrationBundle.message("toolWindow.projects.action.stop"), AllIcons.Actions.Pause);
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return super.isEnabledFor(selected) && selected.isRunning();
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            DdevRunner.getInstance().stopProject(DdevProjectsPanel.this.ideProject, Objects.requireNonNull(selected.getName()), DdevProjectsPanel.this::refreshLater);
        }
    }

    private final class RestartAction extends SelectionAwareAction {
        RestartAction() {
            super(DdevIntegrationBundle.message("toolWindow.projects.action.restart"), AllIcons.Actions.Refresh);
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return super.isEnabledFor(selected) && selected.isRunning();
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            DdevRunner.getInstance().restartProject(DdevProjectsPanel.this.ideProject, Objects.requireNonNull(selected.getName()), DdevProjectsPanel.this::refreshLater);
        }
    }

    private final class StopOthersAction extends SelectionAwareAction {
        StopOthersAction() {
            super(DdevIntegrationBundle.message("toolWindow.projects.action.stopOthers"), AllIcons.Actions.Suspend);
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return super.isEnabledFor(selected) && DdevProjectsPanel.this.collectOtherRunning(selected).stream().findAny().isPresent();
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            final List<String> otherRunning = DdevProjectsPanel.this.collectOtherRunning(selected);
            DdevRunner.getInstance().stopProjects(DdevProjectsPanel.this.ideProject, otherRunning, DdevProjectsPanel.this::refreshLater);
        }
    }

    private @NotNull List<String> collectOtherRunning(@NotNull DdevProject selected) {
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
            final String appRoot = selected.getAppRoot();

            if (binary == null || appRoot == null) {
                return;
            }

            new Task.Backgroundable(DdevProjectsPanel.this.ideProject, DdevIntegrationBundle.message("snapshot.loading"), true) {
                private @Nullable List<Snapshot> snapshots;

                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    try {
                        this.snapshots = Ddev.getInstance().listSnapshots(binary, DdevProjectsPanel.this.ideProject, appRoot).stream()
                                .sorted(Comparator.comparing(Snapshot::getCreated, Comparator.nullsLast(Comparator.reverseOrder())))
                                .toList();
                    } catch (CommandFailedException exception) {
                        DdevNotifier.getInstance(DdevProjectsPanel.this.ideProject).notifySnapshotListFailed();
                    }
                }

                @Override
                public void onSuccess() {
                    if (this.snapshots == null) {
                        return;
                    }

                    if (this.snapshots.isEmpty()) {
                        Messages.showInfoMessage(DdevProjectsPanel.this.ideProject,
                                DdevIntegrationBundle.message("snapshot.none.message"),
                                DdevIntegrationBundle.message("snapshot.restore.popupTitle"));
                        return;
                    }

                    JBPopupFactory.getInstance()
                            .createPopupChooserBuilder(this.snapshots)
                            .setTitle(DdevIntegrationBundle.message("snapshot.restore.popupTitle"))
                            .setRenderer(new SimpleListCellRenderer<Snapshot>() {
                                @Override
                                public void customize(@NotNull JList<? extends Snapshot> list, Snapshot snapshot, int index, boolean isSelected, boolean cellHasFocus) {
                                    this.setText(snapshot.getName());
                                }
                            })
                            .setNamerForFiltering(Snapshot::getName)
                            .setItemChosenCallback(snapshot -> {
                                if (snapshot.getName() != null) {
                                    DdevRunner.getInstance().restoreSnapshot(DdevProjectsPanel.this.ideProject, appRoot, snapshot.getName());
                                }
                            })
                            .createPopup()
                            .showCenteredInCurrentWindow(DdevProjectsPanel.this.ideProject);
                }
            }.queue();
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
                DdevRunner.getInstance().importDatabase(DdevProjectsPanel.this.ideProject, selected.getAppRoot(), file.getPath());
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

    private final class OpenBrowserAction extends SelectionAwareAction {
        OpenBrowserAction() {
            super(DdevIntegrationBundle.message("toolWindow.projects.action.openBrowser"), AllIcons.General.Web);
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return selected != null && selected.isRunning() && selected.getPrimaryUrl() != null;
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            if (selected.getPrimaryUrl() != null) {
                BrowserUtil.browse(selected.getPrimaryUrl());
            }
        }
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
                DdevRunner.getInstance().renameProject(DdevProjectsPanel.this.ideProject, selected.getAppRoot(), newName.trim(), DdevProjectsPanel.this::refreshLater);
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
                DdevRunner.getInstance().deleteProject(DdevProjectsPanel.this.ideProject, Objects.requireNonNull(selected.getName()), DdevProjectsPanel.this::refreshLater);
            }
        }
    }
}
