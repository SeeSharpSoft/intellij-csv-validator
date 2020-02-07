package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

import javax.swing.table.DefaultTableModel;
import java.util.Vector;

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
        DefaultTableModel tableModel = fileEditor.getTableModel();
        assertEquals(2, tableModel.getColumnCount());
        assertEquals(5, tableModel.getRowCount());

        Vector columns = (Vector)tableModel.getDataVector().get(0);
        assertEquals("Header1", columns.get(0));
        assertEquals("Header2", columns.get(1));
        columns = (Vector)tableModel.getDataVector().get(1);
        assertEquals("", columns.get(0));
        assertEquals("", columns.get(1));
        columns = (Vector)tableModel.getDataVector().get(2);
        assertEquals("after the empty line", columns.get(0));
        assertEquals("", columns.get(1));
        columns = (Vector)tableModel.getDataVector().get(3);
        assertEquals("before the previous last line", columns.get(0));
        assertEquals("", columns.get(1));
        columns = (Vector)tableModel.getDataVector().get(4);
        assertEquals("", columns.get(0));
        assertEquals("", columns.get(1));
    }
}
