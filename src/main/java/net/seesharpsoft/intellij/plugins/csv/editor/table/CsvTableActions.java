package net.seesharpsoft.intellij.plugins.csv.editor.table;

import org.jetbrains.annotations.NotNull;

public interface CsvTableActions<T extends CsvTableEditor> {

    CsvTableActions DUMMY = new CsvTableActions() {
        @Override
        public void addRow(CsvTableEditor tableEditor, boolean before) { }

        @Override
        public void addColumn(CsvTableEditor tableEditor, boolean before) { }

        @Override
        public void deleteSelectedRows(CsvTableEditor tableEditor) { }

        @Override
        public void deleteSelectedColumns(CsvTableEditor tableEditor) { }

        @Override
        public void clearSelectedCells(CsvTableEditor tableEditor) { }
    };

    void addRow(@NotNull T tableEditor, boolean before);

    void addColumn(@NotNull T tableEditor, boolean before);

    void deleteSelectedRows(@NotNull T tableEditor);

    void deleteSelectedColumns(@NotNull T tableEditor);

    void clearSelectedCells(@NotNull T tableEditor);

    default void adjustColumnWidths(@NotNull T tableEditor) {
        tableEditor.adjustAllColumnWidths();
        tableEditor.updateEditorLayout();
    }

    default void resetColumnWidths(@NotNull T tableEditor) {
        tableEditor.resetAllColumnWidths();
        tableEditor.updateEditorLayout();
    }
}
