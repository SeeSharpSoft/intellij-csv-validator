package net.seesharpsoft.intellij.plugins.csv.editor;

import com.intellij.openapi.fileEditor.FileEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A CSV editor view that has a selected field, e.g. the field at the text caret or the focused table cell.
 * Callers must ensure the file's document is committed.
 */
public interface CsvFieldView extends FileEditor {

    @Nullable CsvFieldPosition getSelectedField();

    /**
     * Selects and reveals the field, keeping the current selection if it is already on that field.
     */
    void selectField(@NotNull CsvFieldPosition field);
}
