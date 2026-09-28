package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.ide.BrowserUtil;
import com.intellij.notification.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.options.ShowSettingsUtil;
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
        if (project == null || project.isDisposed()) return;

        if (link.startsWith("#")) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                ApplicationManager.getApplication().invokeLater(() ->
                        showSettingsLink(project, link.substring(1))
                );
            });
        } else {
            ApplicationManager.getApplication().invokeLater(() ->
                    BrowserUtil.browse(link, project)
            );
        }
    }

    public static void doAsyncProjectMaintenance(@NotNull Project project) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "CSV Editor validation") {
            public void run(@NotNull ProgressIndicator progressIndicator) {
                cleanupProjectAttributes(project, progressIndicator);
            }
        });
    }

    private static void showSettingsLink(@NotNull Project project, @NotNull String settingsId) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, configurable ->
                matchesSettingsId(configurable, settingsId), null);
    }

    static boolean matchesSettingsId(@NotNull Configurable configurable, @NotNull String settingsId) {
        return configurable instanceof SearchableConfigurable searchableConfigurable &&
                settingsId.equals(searchableConfigurable.getId());
    }

    static void cleanupProjectAttributes(@NotNull Project project, @NotNull ProgressIndicator progressIndicator) {
        progressIndicator.setIndeterminate(false);
        progressIndicator.setFraction(0.50);
        progressIndicator.setText("Validating CSV file attributes");

        try {
            // This maintenance is optional. Do not initialize the persistent service here:
            // loading its state can fail before the project is usable (see #958).
            CsvFileAttributes csvFileAttributes = project.getServiceIfCreated(CsvFileAttributes.class);
            if (csvFileAttributes != null) {
                csvFileAttributes.cleanupAttributeMap(project);
            }
        } catch (Exception exception) {
            // Cleanup is optional and must not prevent project startup.
        }

        progressIndicator.setFraction(1.0);
        progressIndicator.setText("Finished");
    }

    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        doAsyncProjectMaintenance(project);

        ApplicationManager.getApplication().invokeLater(() -> {
            NotificationGroup notificationGroup = NotificationGroupManager.getInstance().getNotificationGroup("net.seesharpsoft.intellij.plugins.csv");
            String version = CsvPluginManager.getVersion();
            if (version.isEmpty() || notificationGroup == null || CsvEditorSettings.getInstance().checkCurrentPluginVersion(version)) {
                return;
            }

            Notification notification = notificationGroup.createNotification(
                    "CSV Editor " + version + " - Change Notes",
                    CsvPluginManager.getChangeNotes() +
                            "<p>You can always <b>customize plugin settings</b> to your likings (shortcuts below)!</p>" +
                            "<br>" +
                            "<p>Visit the <b>CSV Editor homepage</b> to read more about the available features & settings, " +
                            "submit issues & feature request, " +
                            "or show your support by rating this plugin. <b>Thanks!</b></p>"
                    ,
                    NotificationType.INFORMATION
            );

            notification.addAction(NotificationAction.create("General settings", (anActionEvent, notification1) -> {
                openLink(project, "#" + CsvEditorSettingsProvider.CSV_EDITOR_SETTINGS_ID);
            }));
            notification.addAction(NotificationAction.create("Color scheme", (anActionEvent, notification1) -> {
                openLink(project, "#reference.settingsdialog.IDE.editor.colors.CSV/TSV/PSV");
            }));
            notification.addAction(NotificationAction.create("Formatting", (anActionEvent, notification1) -> {
                openLink(project, "#preferences.sourceCode.CSV/TSV/PSV");
            }));
            notification.addAction(NotificationAction.create("Open CSV Editor homepage", (anActionEvent, notification1) -> {
                openLink(project, "https://github.com/SeeSharpSoft/intellij-csv-validator");
            }));

            Notifications.Bus.notify(notification);
        });

        return continuation;
    }
}
