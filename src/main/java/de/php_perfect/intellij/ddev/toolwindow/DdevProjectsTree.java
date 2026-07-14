package de.php_perfect.intellij.ddev.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.DdevProject;
import de.php_perfect.intellij.ddev.cmd.Service;
import de.php_perfect.intellij.ddev.icons.DdevIntegrationIcons;
import de.php_perfect.intellij.ddev.settings.DdevSettingsState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

final class DdevProjectsTree {
    private DdevProjectsTree() {
    }

    static final class UrlItem {
        final @NotNull String url;
        UrlItem(@NotNull String url) { this.url = url; }
    }

    static final class PathItem {
        final @NotNull String displayPath;
        final @Nullable String fullPath;
        PathItem(@NotNull String displayPath, @Nullable String fullPath) {
            this.displayPath = displayPath;
            this.fullPath = fullPath;
        }
    }

    static final class ServicesGroup {
        final @NotNull String projectName;
        boolean loaded;
        ServicesGroup(@NotNull String projectName) { this.projectName = projectName; }
    }

    static final class ServiceItem {
        final @NotNull String name;
        final @NotNull Service service;
        ServiceItem(@NotNull String name, @NotNull Service service) {
            this.name = name;
            this.service = service;
        }
        @Nullable String getUrl() { return this.service.getPreferredUrl(); }
    }

    static final class LoadingItem {
    }

    static final class Renderer extends ColoredTreeCellRenderer {
        private final @NotNull Project project;

        Renderer(@NotNull Project project) {
            this.project = project;
        }

        @Override
        public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded,
                                          boolean leaf, int row, boolean hasFocus) {
            final Object item = ((DefaultMutableTreeNode) value).getUserObject();
            if (item instanceof DdevProject ddevProject) {
                this.setIcon(DdevIntegrationIcons.DdevLogoMono);
                final String name = ddevProject.getName() == null ? "?"
                        : DdevProjectNameFormatter.format(ddevProject.getName(),
                        DdevSettingsState.getInstance(this.project).projectNameFormat);
                this.append(name, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                if (ddevProject.getStatusDesc() != null) {
                    this.append("  " + ddevProject.getStatusDesc(), ddevProject.isRunning()
                            ? new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN,
                            com.intellij.ui.JBColor.namedColor("Label.successForeground",
                                    new com.intellij.ui.JBColor(0x368746, 0x50A661)))
                            : SimpleTextAttributes.GRAYED_ATTRIBUTES);
                }
                if (ddevProject.getType() != null) {
                    this.append("  [" + ddevProject.getType() + "]", SimpleTextAttributes.GRAYED_ATTRIBUTES);
                }
            } else if (item instanceof UrlItem urlItem) {
                this.setIcon(AllIcons.General.Web);
                this.append(urlItem.url, SimpleTextAttributes.LINK_ATTRIBUTES);
            } else if (item instanceof PathItem pathItem) {
                this.setIcon(AllIcons.Nodes.Folder);
                this.append(pathItem.displayPath, SimpleTextAttributes.REGULAR_ATTRIBUTES);
            } else if (item instanceof ServicesGroup) {
                this.setIcon(AllIcons.Nodes.PpLib);
                this.append(DdevIntegrationBundle.message("toolWindow.projects.node.services"),
                        SimpleTextAttributes.REGULAR_ATTRIBUTES);
            } else if (item instanceof ServiceItem serviceItem) {
                this.setIcon(AllIcons.Nodes.Plugin);
                this.append(serviceItem.name, SimpleTextAttributes.REGULAR_ATTRIBUTES);
                if (serviceItem.getUrl() != null) {
                    this.append("  " + serviceItem.getUrl(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
                }
            } else if (item instanceof LoadingItem) {
                this.append(DdevIntegrationBundle.message("toolWindow.projects.node.loading"),
                        SimpleTextAttributes.GRAYED_ATTRIBUTES);
            }
        }
    }
}
