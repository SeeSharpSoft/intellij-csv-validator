package net.seesharpsoft.intellij.plugins.csv.editor.table.api;

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditor;
import net.seesharpsoft.intellij.plugins.csv.editor.table.swing.CsvTableEditorSwing;

import static org.fest.assertions.Assertions.assertThat;

public class TableDataHandlerTest  extends LightCodeInsightFixtureTestCase {

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
        CsvTableEditor csvTableEditor = new CsvTableEditorSwing(this.getProject(), this.getFile().getVirtualFile());
        TableDataHandler handler = new TableDataHandler(csvTableEditor, 2);

        assertThat(handler.tableEditor).isEqualTo(csvTableEditor);
        assertThat(handler.maxSize).isEqualTo(2);
    }

    public void testAddState() {
        CsvTableEditor csvTableEditor = new CsvTableEditorSwing(this.getProject(), this.getFile().getVirtualFile());
        TableDataHandler handler = new TableDataHandler(csvTableEditor, 4);

        Object[][] values = { { "Test", 1 }, { 2, 5} };
        handler.addState(values);

        assertThat(handler.getCurrentState()).isEqualTo(values);

        handler.addState(values);

        assertThat(handler.states.size()).isEqualTo(1);

        Object[][] otherValues = { { 3, 4 }, { "Test", 0} };
        handler.addState(otherValues);
        Object[][] otherValues2 = { { null } };
        handler.addState(otherValues2);

        assertThat(handler.states.size()).isEqualTo(3);

        handler.getLastState();
        handler.getLastState();

        Object[][] otherValues3 = { { "dummy", 7, 9, 11 } };
        handler.addState(otherValues3);

        assertThat(handler.states.size()).isEqualTo(2);
    }

    public void testGetCurrentState() {
        CsvTableEditor csvTableEditor = new CsvTableEditorSwing(this.getProject(), this.getFile().getVirtualFile());
        TableDataHandler handler = new TableDataHandler(csvTableEditor, 2);

        Object[][] values = { { "Test", 1 }, { 2, 5} };
        handler.addState(values);

        assertThat(handler.getCurrentState()).isNotSameAs(values);
    }

    public void testEqualsCurrentState() {
        CsvTableEditor csvTableEditor = new CsvTableEditorSwing(this.getProject(), this.getFile().getVirtualFile());
        TableDataHandler handler = new TableDataHandler(csvTableEditor, 2);

        Object[][] values = { { "Test", 1 }, { 2, 5} };
        handler.addState(values);

        assertThat(handler.equalsCurrentState(values)).isEqualTo(true);
        Object[][] otherValues = { { 3, 4 }, { "Test", 0} };
        assertThat(handler.equalsCurrentState(otherValues)).isEqualTo(false);
    }

    public void testGetLastState() {
        CsvTableEditor csvTableEditor = new CsvTableEditorSwing(this.getProject(), this.getFile().getVirtualFile());
        TableDataHandler handler = new TableDataHandler(csvTableEditor, 2);

        assertThat(handler.getLastState()).isNull();

        Object[][] values = { { "Test", 1 }, { 2, 5} };
        handler.addState(values);

        assertThat(handler.getLastState()).isNull();

        Object[][] otherValues = { { 3, 4 }, { "Test", 0} };
        handler.addState(otherValues);

        assertThat(handler.getLastState()).isEqualTo(values);
        assertThat(handler.getLastState()).isNull();
    }

    public void testCanGetLastState() {
        CsvTableEditor csvTableEditor = new CsvTableEditorSwing(this.getProject(), this.getFile().getVirtualFile());
        TableDataHandler handler = new TableDataHandler(csvTableEditor, 2);

        assertThat(handler.canGetLastState()).isEqualTo(false);

        Object[][] values = { { "Test", 1 }, { 2, 5} };
        handler.addState(values);

        assertThat(handler.canGetLastState()).isEqualTo(false);

        Object[][] otherValues = { { 3, 4 }, { "Test", 0} };
        handler.addState(otherValues);

        assertThat(handler.canGetLastState()).isEqualTo(true);

        handler.getLastState();

        assertThat(handler.canGetLastState()).isEqualTo(false);
    }

    public void testGetNextState() {
        CsvTableEditor csvTableEditor = new CsvTableEditorSwing(this.getProject(), this.getFile().getVirtualFile());
        TableDataHandler handler = new TableDataHandler(csvTableEditor, 2);

        assertThat(handler.getNextState()).isNull();

        Object[][] values = { { "Test", 1 }, { 2, 5} };
        handler.addState(values);

        assertThat(handler.getNextState()).isNull();

        Object[][] otherValues = { { 3, 4 }, { "Test", 0} };
        handler.addState(otherValues);

        assertThat(handler.getNextState()).isNull();

        handler.getLastState();

        assertThat(handler.getNextState()).isEqualTo(otherValues);
        assertThat(handler.getNextState()).isNull();
    }

    public void testMaxSize() {
        CsvTableEditor csvTableEditor = new CsvTableEditorSwing(this.getProject(), this.getFile().getVirtualFile());
        TableDataHandler handler = new TableDataHandler(csvTableEditor, 2);

        Object[][] values = { { "Test", 1 }, { 2, 5} };
        handler.addState(values);
        Object[][] otherValues = { { 3, 4 }, { "Test", 0} };
        handler.addState(otherValues);
        Object[][] otherValues2 = { { null } };
        handler.addState(otherValues2);

        assertThat(handler.getLastState()).isEqualTo(otherValues);
        assertThat(handler.getLastState()).isNull();
    }

    public void testDataChangeListener() {
        CsvTableEditor csvTableEditor = new CsvTableEditorSwing(this.getProject(), this.getFile().getVirtualFile());
        TableDataHandler handler = new TableDataHandler(csvTableEditor, 2);

        Listener listener = new Listener();
        handler.addDataChangeListener(listener);

        Object[][] values = { { "Test", 1 }, { 2, 5} };
        handler.addState(values);
        assertThat(listener.noOfCalls).isEqualTo(1);

        handler.addState(values);
        assertThat(listener.noOfCalls).isEqualTo(1);

        Object[][] otherValues = { { 3, 4 }, { "Test", 0} };
        handler.addState(otherValues);
        Object[][] otherValues2 = { { null } };
        handler.addState(otherValues2);
        assertThat(listener.noOfCalls).isEqualTo(3);

        handler.getNextState();
        assertThat(listener.noOfCalls).isEqualTo(3);

        handler.getLastState();
        assertThat(listener.noOfCalls).isEqualTo(4);

        handler.getLastState();
        assertThat(listener.noOfCalls).isEqualTo(4);

        handler.getNextState();
        assertThat(listener.noOfCalls).isEqualTo(5);

        handler.removeDataChangeListener(listener);
        handler.addState(values);
        handler.getNextState();
        handler.getLastState();
        assertThat(listener.noOfCalls).isEqualTo(5);
    }

    private class Listener implements TableDataChangeEvent.Listener {
        public int noOfCalls = 0;
        @Override
        public void onTableDataChanged(TableDataChangeEvent event) {
            ++noOfCalls;
        }
    }
}
