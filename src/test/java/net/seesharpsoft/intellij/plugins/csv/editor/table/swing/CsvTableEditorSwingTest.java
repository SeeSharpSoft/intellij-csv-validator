package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.util.Key;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditor;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditorState;

import javax.swing.table.DefaultTableModel;
import java.util.Vector;

public class CsvTableEditorSwingTest extends CsvTableEditorSwingTestBase {

    public void testBasics() {
        assertEquals(CsvTableEditor.EDITOR_NAME, fileEditor.getName());
        assertEquals(fileEditor, fileEditor.getCurrentLocation());
        assertInstanceOf(fileEditor.getState(FileEditorStateLevel.FULL), CsvTableEditorState.class);
        assertInstanceOf(fileEditor.getState(FileEditorStateLevel.NAVIGATION), CsvTableEditorState.class);
        assertInstanceOf(fileEditor.getState(FileEditorStateLevel.UNDO), CsvTableEditorState.class);

        CsvTableEditorState myState = new CsvTableEditorState();
        fileEditor.setState(myState);
        assertEquals(myState, fileEditor.getState(FileEditorStateLevel.FULL));

        assertEquals(false, fileEditor.isModified());
        assertEquals(true, fileEditor.isValid());
        assertEquals(null, fileEditor.getBackgroundHighlighter());

        StructureViewBuilder structureViewBuilder = StructureViewBuilder.PROVIDER.getStructureViewBuilder(this.getFile().getFileType(), this.getFile().getVirtualFile(), this.getProject());
        assertInstanceOf(fileEditor.getStructureViewBuilder(), structureViewBuilder.getClass());

        assertEquals(this.getFile().getVirtualFile(), fileEditor.getFile());
        assertEquals(this.getProject(), fileEditor.getProject());
        assertNotNull(fileEditor.getDataHandler());
        assertNotNull(fileEditor.getComponent());
        assertEquals(fileEditor.getTable(), fileEditor.getPreferredFocusedComponent());
    }

    public void testEditable() {
        assertEquals(true, fileEditor.isEditable());
        fileEditor.setEditable(false);
        assertEquals(false, fileEditor.isEditable());
        fileEditor.setEditable(true);
        assertEquals(true, fileEditor.isEditable());
    }

    public void testUserDataHolder() {
        Key<String> testKey = new Key<>("myKey");
        String value = "This is just a test";
        assertNull(fileEditor.getUserData(testKey));
        fileEditor.putUserData(testKey, value);
        assertEquals(value, fileEditor.getUserData(testKey));
    }

    public void testTableContent() {
        DefaultTableModel tableModel = fileEditor.getTableModel();
        assertEquals(2, tableModel.getColumnCount());
        assertEquals(4, tableModel.getRowCount());

        Vector columns = (Vector)tableModel.getDataVector().get(0);
        assertEquals("Header1", columns.get(0));
        assertEquals("header 2", columns.get(1));
        columns = (Vector)tableModel.getDataVector().get(1);
        assertEquals("this is column \"Header1\"", columns.get(0));
        assertEquals("this is column header 2", columns.get(1));
        columns = (Vector)tableModel.getDataVector().get(2);
        assertEquals("just another line with leading and trailing whitespaces", columns.get(0));
        assertEquals("  and one more value  ", columns.get(1));
        columns = (Vector)tableModel.getDataVector().get(3);
        assertEquals("", columns.get(0));
        assertEquals(null, columns.get(1));
    }

    public void testTableContentChanges() {
        Object[][] newState = changeValue("new value", 2, 1);
        assertTrue(fileEditor.isModified());
        assertFalse(fileEditor.getDataHandler().equalsCurrentState(initialState));
        assertTrue(fileEditor.getDataHandler().equalsCurrentState(newState));
    }
}
