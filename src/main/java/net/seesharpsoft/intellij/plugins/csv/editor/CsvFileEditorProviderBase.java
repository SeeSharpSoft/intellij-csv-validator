package net.seesharpsoft.intellij.plugins.csv.editor;

import com.intellij.openapi.fileEditor.AsyncFileEditorProvider;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.WeighedFileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.jetbrains.annotations.NotNull;

/**
 * Orders the CSV text and table editors according to the editor priority setting.
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