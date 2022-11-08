package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.ide.actions.ShowSettingsUtilImpl;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.ide.ui.IdeUiService;
import com.intellij.notification.*;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import net.seesharpsoft.intellij.plugins.csv.components.CsvFileAttributes;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettingsProvider;
import org.jetbrains.annotations.NotNull;

public class CsvPlugin implements StartupActivity, StartupActivity.DumbAware, StartupActivity.Background {

    protected static IdeaPluginDescriptor getPluginDescriptor() {
        return PluginManagerCore.getPlugin(PluginId.getId("net.seesharpsoft.intellij.plugins.csv"));
    }

    protected static String getVersion() {
        return getPluginDescriptor().getVersion();
    }

    protected static String getChangeNotes() {
        return getPluginDescriptor().getChangeNotes();
    }

    private static void openLink(Project project, String link) {
        if (project.isDisposed()) return;

        if (link.startsWith("#")) {
            ((ShowSettingsUtilImpl) ShowSettingsUtil.getInstance()).showSettingsDialog(project, link.substring(1), null);
        } else {
            IdeUiService.getInstance().browse(link);
        }
    }

    public static void doAsyncProjectMaintenance(@NotNull Project project) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "CSV plugin validation") {
            public void run(@NotNull ProgressIndicator progressIndicator) {
                // initialize progress indication
                progressIndicator.setIndeterminate(false);

                // Set the progress bar percentage and text
                progressIndicator.setFraction(0.50);
                progressIndicator.setText("Validating CSV file attributes");

                // start process
                CsvFileAttributes csvFileAttributes = CsvFileAttributes.getInstance(project);
                csvFileAttributes.cleanupAttributeMap(project);

                // finished
                progressIndicator.setFraction(1.0);
                progressIndicator.setText("finished");
            }
        });
    }

    @Override
    public void runActivity(@NotNull Project project) {
        doAsyncProjectMaintenance(project);

        if (CsvEditorSettings.getInstance().checkCurrentPluginVersion(getVersion())) {
            return;
        }

        NotificationGroup notificationGroup = NotificationGroupManager.getInstance().getNotificationGroup("net.seesharpsoft.intellij.plugins.csv");
        Notification notification = notificationGroup.createNotification(
                "CSV Plugin " + getVersion() + " - Change Notes",
                getChangeNotes() +
                        "<p>You can always <b>customize plugin settings</b> to your likings (shortcuts below)!</p>" +
                        "<br>" +
                        "<p>Visit the <b>CSV Plugin homepage</b> to read more about the available features & settings, " +
                        "submit issues & feature request, " +
                        "or show your support by rating this plugin. <b>Thanks!</b></p>"
                ,
                NotificationType.INFORMATION
        );

        notification.addAction(NotificationAction.create("General Settings", (anActionEvent, notification1) -> {
            openLink(project, "#" + CsvEditorSettingsProvider.CSV_EDITOR_SETTINGS_ID);
        }));
        notification.addAction(NotificationAction.create("Color Scheme", (anActionEvent, notification1) -> {
            openLink(project, "#reference.settingsdialog.IDE.editor.colors.CSV/TSV/PSV");
        }));
        notification.addAction(NotificationAction.create("Formatting", (anActionEvent, notification1) -> {
            openLink(project, "#preferences.sourceCode.CSV/TSV/PSV");
        }));
        notification.addAction(NotificationAction.create("Open CSV Plugin homepage", (anActionEvent, notification1) -> {
            openLink(project, "https://github.com/SeeSharpSoft/intellij-csv-validator");
        }));

        Notifications.Bus.notify(notification);
    }
}