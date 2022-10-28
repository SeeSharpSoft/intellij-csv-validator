package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import net.seesharpsoft.intellij.plugins.csv.editor.table.api.CsvTableModel;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
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
        CsvTableModel tableModel = fileEditor.getTableModel();
        assertEquals(2, tableModel.getColumnCount());
        assertEquals(5, tableModel.getRowCount());

        assertEquals("Header1", tableModel.getValueAt(0, 0));
        assertEquals("Header2", tableModel.getValueAt(0, 1));
        assertEquals("", tableModel.getValueAt(1, 0));
        assertEquals("", tableModel.getValueAt(1, 1));
        assertEquals("after the empty line", tableModel.getValueAt(2, 0));
        assertEquals("", tableModel.getValueAt(2, 1));
        assertEquals("before the previous last line", tableModel.getValueAt(3, 0));
        assertEquals("", tableModel.getValueAt(3, 1));
        assertEquals("", tableModel.getValueAt(4, 0));
        assertEquals("", tableModel.getValueAt(4, 1));
    }
}
