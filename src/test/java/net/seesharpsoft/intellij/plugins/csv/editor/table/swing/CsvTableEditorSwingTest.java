package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.util.Key;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditor;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditorState;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableModel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class CsvTableEditorSwingTest extends CsvTableEditorSwingTestBase {

    public void testBasics() {
        assertEquals(CsvTableEditor.EDITOR_NAME, fileEditor.getName());
        assertEquals(null, fileEditor.getCurrentLocation());
        assertInstanceOf(fileEditor.getState(FileEditorStateLevel.FULL), CsvTableEditorState.class);
        assertInstanceOf(fileEditor.getState(FileEditorStateLevel.NAVIGATION), CsvTableEditorState.class);
        assertInstanceOf(fileEditor.getState(FileEditorStateLevel.UNDO), CsvTableEditorState.class);

        CsvTableEditorState myState = new CsvTableEditorState();
        fileEditor.setState(myState);
        assertEquals(myState, fileEditor.getState(FileEditorStateLevel.FULL));

        assertEquals(false, fileEditor.isModified());
        assertEquals(true, fileEditor.isValid());
        assertEquals(null, fileEditor.getBackgroundHighlighter());

        assertEquals(myFixture.getFile().getVirtualFile(), fileEditor.getFile());
        assertEquals(this.getProject(), fileEditor.getProject());
        assertNotNull(fileEditor.getComponent());
        assertEquals(fileEditor.getTable(), fileEditor.getPreferredFocusedComponent());
    }

    public void testAddRemovePropertyChangeListener() throws Throwable {
        assertThrows(IllegalArgumentException.class, () -> fileEditor.addPropertyChangeListener(null));
        assertThrows(IllegalArgumentException.class, () -> fileEditor.removePropertyChangeListener(null));

        PropertyChangeListener listener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
            }
        };
        fileEditor.removePropertyChangeListener(listener);
        fileEditor.addPropertyChangeListener(listener);
        fileEditor.removePropertyChangeListener(listener);
        assertTrue(true);
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
        CsvTableModel tableModel = fileEditor.getTableModel();
        assertEquals(2, tableModel.getColumnCount());
        assertEquals(4, tableModel.getRowCount());

        assertEquals("Header1", tableModel.getValue(0, 0));
        assertEquals(" header 2", tableModel.getValue(0, 1));
        assertEquals("this is column \"Header1\"", tableModel.getValue(1, 0));
        assertEquals("this is column header 2", tableModel.getValue(1, 1));
        assertEquals(" just another line with leading and trailing whitespaces  ", tableModel.getValue(2, 0));
        assertEquals("  and one more value  ", tableModel.getValue(2, 1));
        assertEquals("", tableModel.getValue(3, 0));
        assertEquals("", tableModel.getValue(3, 1));
    }
}
