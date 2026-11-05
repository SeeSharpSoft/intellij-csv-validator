package net.seesharpsoft.intellij.plugins.csv.editor;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.AsyncFileEditorProvider;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.WeighedFileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers;
import net.seesharpsoft.intellij.plugins.csv.components.CsvFileAttributes;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Loads persistent CSV state before constructing either editor's UI, and orders the text and table editors
 * according to the editor priority setting.
 * <p>
 * Other plugins (e.g. Big Data Tools, #937) register CSV editors with {@link FileEditorPolicy#HIDE_OTHER_EDITORS},
 * which removes every editor using a different policy. Sharing that policy keeps the CSV editors available,
 * while a weight lower than those of other editors places them first.
 */
public abstract class CsvFileEditorProviderBase extends WeighedFileEditorProvider implements AsyncFileEditorProvider, DumbAware {

    private static final double PRIMARY_EDITOR_WEIGHT = 0.1;
    private static final double SECONDARY_EDITOR_WEIGHT = 0.2;

    protected abstract boolean isPrimaryEditor(@NotNull CsvEditorSettings.EditorPrio editorPrio);

    @Override
    public @Nullable Object createFileEditor(@NotNull Project project, @NotNull VirtualFile file,
                                             @Nullable Document document, @NotNull CoroutineScope editorCoroutineScope,
                                             @NotNull Continuation<? super FileEditor> continuation) {
        // State storage can use remote I/O. Finish loading before the platform's read action and EDT builder.
        return BuildersKt.withContext(Dispatchers.getIO(), (scope, backgroundContinuation) -> {
            CsvFileAttributes.getInstance(project);
            return AsyncFileEditorProvider.super.createFileEditor(
                    project, file, document, editorCoroutineScope, backgroundContinuation);
        }, continuation);
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        if (CsvEditorSettings.getInstance().isHideOtherEditors()) {
            return FileEditorPolicy.HIDE_OTHER_EDITORS;
        }
        return isPrimaryEditor() ? FileEditorPolicy.HIDE_DEFAULT_EDITOR : FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR;
    }

    @Override
    public double getWeight() {
        return isPrimaryEditor() ? PRIMARY_EDITOR_WEIGHT : SECONDARY_EDITOR_WEIGHT;
    }

    private boolean isPrimaryEditor() {
        return isPrimaryEditor(CsvEditorSettings.getInstance().getEditorPrio());
    }
}