package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.ide.actions.ShowSettingsUtilImpl;
import com.intellij.ide.plugins.IdeaPluginDescriptorImpl;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.notification.*;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettingsProvider;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.IOException;
import java.net.URI;

public class CsvPlugin implements StartupActivity {

    protected static IdeaPluginDescriptorImpl getPluginDescriptor() {
        return (IdeaPluginDescriptorImpl)PluginManager.getPlugin(PluginId.getId("net.seesharpsoft.intellij.plugins.csv"));
    }

    protected static String getVersion() {
        return getPluginDescriptor().getVersion();
    }

    protected static String getChangeNotes() {
        return getPluginDescriptor().getChangeNotes();
    }

    private static void openLink(Project project, String link) {
        if (!project.isDisposed() && link.startsWith("#")) {
            ((ShowSettingsUtilImpl)ShowSettingsUtil.getInstance()).showSettingsDialog(project, link.substring(1), null);
        }
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(URI.create(link));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void runActivity(@NotNull Project project) {
        if (CsvEditorSettings.getInstance().checkCurrentPluginVersion(getVersion())) {
            return;
        }

        NotificationGroup notificationGroup = new NotificationGroup(
                "CsvPlugin", NotificationDisplayType.STICKY_BALLOON, true
        );

        NotificationListener.Adapter notificationListener = new NotificationListener.Adapter() {
            @Override
            protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent e) {
                openLink(project, e.getDescription());
            }
        };

        Notification notification = notificationGroup.createNotification(
                "CSV Plugin " + getVersion() + " - Change Notes",
                getChangeNotes() +
                "<p><b>Customize plugin settings:</b> " +
                        "<a href=\"#" + CsvEditorSettingsProvider.CSV_EDITOR_SETTINGS_ID + "\">Editor/General</a>, " +
                        "<a href=\"#reference.settingsdialog.IDE.editor.colors.CSV/TSV/PSV\">Color Scheme</a>, " +
                        "<a href=\"#preferences.sourceCode.CSV/TSV/PSV\">Formatting</a></p>" +
                        "<br>" +
                        "<p>Visit the <a href=\"https://github.com/SeeSharpSoft/intellij-csv-validator\">CSV Plugin GitHub</a> to read more about the available features & settings, " +
                        "submit <a href=\"https://github.com/SeeSharpSoft/intellij-csv-validator/issues\">issues & feature request</a>, " +
                        "or show your support by <a href=\"https://plugins.jetbrains.com/plugin/10037-csv-plugin\">rating this plugin</a>. <b>Thanks!</b></p>"
                ,
                NotificationType.INFORMATION,
                notificationListener
        );

        Notifications.Bus.notify(notification);
    }
}