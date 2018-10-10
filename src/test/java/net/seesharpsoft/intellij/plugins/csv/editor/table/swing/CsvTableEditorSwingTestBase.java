package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.editor.CsvEditorSettingsExternalizable;

public abstract class CsvTableEditorSwingTestBase extends LightCodeInsightFixtureTestCase {

    protected CsvTableEditorSwing fileEditor;

    protected Object[][] initialState;

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/editor";
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CsvEditorSettingsExternalizable.getInstance().loadState(new CsvEditorSettingsExternalizable.OptionSet());
        myFixture.configureByFiles("TableEditorFile.csv");

        fileEditor = new CsvTableEditorSwing(this.getProject(), this.getFile().getVirtualFile());
        fileEditor.selectNotify();
        initialState = fileEditor.getDataHandler().getCurrentState();
    }

    @Override
    protected void tearDown() throws Exception {
        fileEditor.dispose();

        super.tearDown();
    }

    protected Object[][] changeValue(String newValue, int row, int column) {
        fileEditor.getTable().setValueAt(newValue, row, column);
        Object[][] copy = CsvHelper.deepCopy(initialState);
        copy[row][column] = newValue;
        return copy;
    }
}
