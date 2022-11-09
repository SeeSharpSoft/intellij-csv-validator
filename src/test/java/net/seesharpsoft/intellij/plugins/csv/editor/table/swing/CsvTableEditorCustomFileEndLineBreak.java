package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableModel;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

public class CsvTableEditorCustomFileEndLineBreak extends CsvTableEditorSwingTestBase {

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/fileendlinebreak";
    }

    @Override
    protected void initializeEditorSettings(CsvEditorSettings instance) {
        super.initializeEditorSettings(instance);
        instance.setFileEndLineBreak(true);
    }

    public void testTableContentChangeWithoutChange() {
        assertFalse(fileEditor.isModified());
    }

    public void testTableContent() {
        CsvTableModel tableModel = fileEditor.getTableModel();
        assertEquals(2, tableModel.getColumnCount());
        assertEquals(5, tableModel.getRowCount());

        assertEquals("Header1", tableModel.getValue(0, 0));
        assertEquals("Header2", tableModel.getValue(0, 1));
        assertEquals("", tableModel.getValue(1, 0));
        assertEquals("", tableModel.getValue(1, 1));
        assertEquals("after the empty line", tableModel.getValue(2, 0));
        assertEquals("", tableModel.getValue(2, 1));
        assertEquals("before the previous last line", tableModel.getValue(3, 0));
        assertEquals("", tableModel.getValue(3, 1));
        assertEquals("", tableModel.getValue(4, 0));
        assertEquals("", tableModel.getValue(4, 1));
    }
}
