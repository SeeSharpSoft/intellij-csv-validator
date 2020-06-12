package net.seesharpsoft.intellij.plugins.csv.editor.table.api;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditor;
import net.seesharpsoft.intellij.plugins.csv.editor.table.swing.CsvTableEditorSwing;

import java.util.Arrays;


public class TableDataHandlerTest extends BasePlatformTestCase {

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/editor";
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.configureByFiles("AnyFile.csv");
    }

    public void testConstructor() {
        CsvTableEditor csvTableEditor = new CsvTableEditorSwing(this.getProject(), myFixture.getFile().getVirtualFile());
        TableDataHandler handler = new TableDataHandler(csvTableEditor, 2);

        assertEquals(handler.tableEditor, csvTableEditor);
        assertEquals(2, handler.maxSize);
    }

    public void testAddState() {
        CsvTableEditor csvTableEditor = new CsvTableEditorSwing(this.getProject(), myFixture.getFile().getVirtualFile());
        TableDataHandler handler = new TableDataHandler(csvTableEditor, 4);

        Object[][] values = { { "Test", 1 }, { 2, 5} };
        handler.addState(values);

        assertTrue(handler.equalsCurrentState(values));

        handler.addState(values);

        assertEquals(1, handler.states.size());

        Object[][] otherValues = { { 3, 4 }, { "Test", 0} };
        handler.addState(otherValues);
        Object[][] otherValues2 = { { null } };
        handler.addState(otherValues2);

        assertEquals(3, handler.states.size());

        handler.getLastState();
        handler.getLastState();

        Object[][] otherValues3 = { { "dummy", 7, 9, 11 } };
        handler.addState(otherValues3);

        assertEquals(2, handler.states.size());
    }

    public void testGetCurrentState() {
        CsvTableEditor csvTableEditor = new CsvTableEditorSwing(this.getProject(), myFixture.getFile().getVirtualFile());
        TableDataHandler handler = new TableDataHandler(csvTableEditor, 2);

        Object[][] values = { { "Test", 1 }, { 2, 5} };
        handler.addState(values);

        assertNotSame(values, handler.getCurrentState());
    }

    public void testEqualsCurrentState() {
        CsvTableEditor csvTableEditor = new CsvTableEditorSwing(this.getProject(), myFixture.getFile().getVirtualFile());
        TableDataHandler handler = new TableDataHandler(csvTableEditor, 2);

        Object[][] values = { { "Test", 1 }, { 2, 5} };
        handler.addState(values);

        assertTrue(handler.equalsCurrentState(values));
        Object[][] otherValues = { { 3, 4 }, { "Test", 0} };
        assertFalse(handler.equalsCurrentState(otherValues));
    }

    public void testGetLastState() {
        CsvTableEditor csvTableEditor = new CsvTableEditorSwing(this.getProject(), myFixture.getFile().getVirtualFile());
        TableDataHandler handler = new TableDataHandler(csvTableEditor, 2);

        assertNull(handler.getLastState());

        Object[][] values = { { "Test", 1 }, { 2, 5} };
        handler.addState(values);

        assertNull(handler.getLastState());

        Object[][] otherValues = { { 3, 4 }, { "Test", 0} };
        handler.addState(otherValues);

        assertTrue(Arrays.deepEquals(values, handler.getLastState()));
        assertNull(handler.getLastState());
    }

    public void testCanGetLastState() {
        CsvTableEditor csvTableEditor = new CsvTableEditorSwing(this.getProject(), myFixture.getFile().getVirtualFile());
        TableDataHandler handler = new TableDataHandler(csvTableEditor, 2);

        assertFalse(handler.canGetLastState());

        Object[][] values = { { "Test", 1 }, { 2, 5} };
        handler.addState(values);

        assertFalse(handler.canGetLastState());

        Object[][] otherValues = { { 3, 4 }, { "Test", 0} };
        handler.addState(otherValues);

        assertTrue(handler.canGetLastState());

        handler.getLastState();

        assertFalse(handler.canGetLastState());
    }

    public void testGetNextState() {
        CsvTableEditor csvTableEditor = new CsvTableEditorSwing(this.getProject(), myFixture.getFile().getVirtualFile());
        TableDataHandler handler = new TableDataHandler(csvTableEditor, 2);

        assertNull(handler.getNextState());

        Object[][] values = { { "Test", 1 }, { 2, 5} };
        handler.addState(values);

        assertNull(handler.getNextState());

        Object[][] otherValues = { { 3, 4 }, { "Test", 0} };
        handler.addState(otherValues);

        assertNull(handler.getNextState());

        handler.getLastState();

        assertTrue(Arrays.deepEquals(otherValues, handler.getNextState()));
        assertNull(handler.getNextState());
    }

    public void testMaxSize() {
        CsvTableEditor csvTableEditor = new CsvTableEditorSwing(this.getProject(), myFixture.getFile().getVirtualFile());
        TableDataHandler handler = new TableDataHandler(csvTableEditor, 2);

        Object[][] values = { { "Test", 1 }, { 2, 5} };
        handler.addState(values);
        Object[][] otherValues = { { 3, 4 }, { "Test", 0} };
        handler.addState(otherValues);
        Object[][] otherValues2 = { { null } };
        handler.addState(otherValues2);

        assertTrue(Arrays.deepEquals(otherValues, handler.getLastState()));
        assertNull(handler.getLastState());
    }

    public void testDataChangeListener() {
        CsvTableEditor csvTableEditor = new CsvTableEditorSwing(this.getProject(), myFixture.getFile().getVirtualFile());
        TableDataHandler handler = new TableDataHandler(csvTableEditor, 2);

        Listener listener = new Listener();
        handler.addDataChangeListener(listener);

        Object[][] values = { { "Test", 1 }, { 2, 5} };
        handler.addState(values);
        assertEquals(1, listener.noOfCalls);

        handler.addState(values);
        assertEquals(1, listener.noOfCalls);

        Object[][] otherValues = { { 3, 4 }, { "Test", 0} };
        handler.addState(otherValues);
        Object[][] otherValues2 = { { null } };
        handler.addState(otherValues2);
        assertEquals(3, listener.noOfCalls);

        handler.getNextState();
        assertEquals(3, listener.noOfCalls);

        handler.getLastState();
        assertEquals(4, listener.noOfCalls);

        handler.getLastState();
        assertEquals(4, listener.noOfCalls);

        handler.getNextState();
        assertEquals(5, listener.noOfCalls);

        handler.removeDataChangeListener(listener);
        handler.addState(values);
        handler.getNextState();
        handler.getLastState();
        assertEquals(5, listener.noOfCalls);
    }

    private class Listener implements TableDataChangeEvent.Listener {
        public int noOfCalls = 0;
        @Override
        public void onTableDataChanged(TableDataChangeEvent event) {
            ++noOfCalls;
        }
    }
}
