package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.actions.ShowSettingsUtilImpl;
import com.intellij.notification.*;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import net.seesharpsoft.intellij.plugins.csv.components.CsvFileAttributes;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettingsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CsvPlugin implements ProjectActivity, DumbAware {

    private static void openLink(Project project, String link) {
        if (project.isDisposed()) return;

        if (link.startsWith("#")) {
            ShowSettingsUtilImpl.showSettingsDialog(project, link.substring(1), null);
        } else {
            BrowserUtil.browse(link, project);
        }
    }

    public static void doAsyncProjectMaintenance(@NotNull Project project) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "CSV Editor validation") {
            public void run(@NotNull ProgressIndicator progressIndicator) {
                // initialize progress indication
                progressIndicator.setIndeterminate(false);

                // Set the progress bar percentage and text
                progressIndicator.setFraction(0.50);
                progressIndicator.setText("Validating CSV file attributes");

                // start process
                try {
                    CsvFileAttributes csvFileAttributes = CsvFileAttributes.getInstance(getProject());
                    csvFileAttributes.cleanupAttributeMap(getProject());
                } catch (Exception exception) {
                    // repeated unresolved bug-reports when retrieving the component
                    // while this cleanup is an optional and non-critical task
                }
                // finished
                progressIndicator.setFraction(1.0);
                progressIndicator.setText("Finished");
            }
        });
    }

    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        doAsyncProjectMaintenance(project);
        
        NotificationGroup notificationGroup = NotificationGroupManager.getInstance().getNotificationGroup("net.seesharpsoft.intellij.plugins.csv");
        if (notificationGroup == null || CsvEditorSettings.getInstance().checkCurrentPluginVersion(CsvPluginManager.getVersion())) {
            return continuation;
        }

        Notification notification = notificationGroup.createNotification(
                "CSV Editor " + CsvPluginManager.getVersion() + " - Change Notes",
                CsvPluginManager.getChangeNotes() +
                        "<p>You can always <b>customize plugin settings</b> to your likings (shortcuts below)!</p>" +
                        "<br>" +
                        "<p>Visit the <b>CSV Editor homepage</b> to read more about the available features & settings, " +
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
        notification.addAction(NotificationAction.create("Open CSV Editor homepage", (anActionEvent, notification1) -> {
            openLink(project, "https://github.com/SeeSharpSoft/intellij-csv-validator");
        }));

        Notifications.Bus.notify(notification);
        
        return continuation;
    }
}