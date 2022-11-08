package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

public class CsvTableEditorAutoColumnWidthOnOpen extends CsvTableEditorSwingTestBase {

    public void testAutoColumnWidthOnFirstOpen() {
        assertTrue("first column is bigger than second", fileEditor.getTable().getColumnModel().getColumn(0).getWidth() > fileEditor.getTable().getColumnModel().getColumn(1).getWidth());
    }
}
