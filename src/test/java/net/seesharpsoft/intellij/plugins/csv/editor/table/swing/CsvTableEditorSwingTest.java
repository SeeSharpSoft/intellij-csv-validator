package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.util.Key;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditor;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditorState;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

import javax.swing.table.DefaultTableModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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

        StructureViewBuilder structureViewBuilder = StructureViewBuilder.PROVIDER.getStructureViewBuilder(myFixture.getFile().getFileType(), myFixture.getFile().getVirtualFile(), this.getProject());
        assertInstanceOf(fileEditor.getStructureViewBuilder(), structureViewBuilder.getClass());

        assertEquals(myFixture.getFile().getVirtualFile(), fileEditor.getFile());
        assertEquals(this.getProject(), fileEditor.getProject());
        assertNotNull(fileEditor.getDataHandler());
        assertNotNull(fileEditor.getComponent());
        assertEquals(fileEditor.getTable(), fileEditor.getPreferredFocusedComponent());
    }

    public void testAddRemovePropertyChangeListener() throws Throwable {
        assertException(IllegalArgumentException.class, null, () -> fileEditor.addPropertyChangeListener(null));
        assertException(IllegalArgumentException.class, null, () -> fileEditor.removePropertyChangeListener(null));

        PropertyChangeListener listener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) { }
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
        assertEquals("", columns.get(1));
    }

    public void testTableContentChanges() {
        Object[][] newState = changeValue("new value", 2, 1);
        assertTrue(fileEditor.isModified());
        assertFalse(fileEditor.getDataHandler().equalsCurrentState(initialState));
        assertTrue(fileEditor.getDataHandler().equalsCurrentState(newState));
    }

    public void testTableCsvGeneration() throws FileNotFoundException {
        changeValue("new value", 2, 1);
        String generatedCsv = fileEditor.generateCsv(fileEditor.getDataHandler().getCurrentState());

        File resultFile = new File(this.getTestDataPath(), "TableEditorFileChanged.csv");
        String expectedContent = new BufferedReader(new FileReader(resultFile)).lines().reduce(null, (prev, line) -> prev == null ? line : prev + "\n" + line);

        assertEquals(expectedContent, generatedCsv);
    }

    public void testTableCsvGenerationEnforceQuoting() throws FileNotFoundException {
        changeValue("new value", 2, 1);
        CsvEditorSettings.getInstance().setQuotingEnforced(true);
        String generatedCsv = fileEditor.generateCsv(fileEditor.getDataHandler().getCurrentState());

        File resultFile = new File(this.getTestDataPath(), "TableEditorFileChangedQuoted.csv");
        String expectedContent = new BufferedReader(new FileReader(resultFile)).lines().reduce(null, (prev, line) -> prev == null ? line : prev + "\n" + line);

        assertEquals(expectedContent, generatedCsv);
    }

}
