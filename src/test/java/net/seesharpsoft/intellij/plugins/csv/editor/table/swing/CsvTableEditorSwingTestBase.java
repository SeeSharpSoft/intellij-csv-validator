package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

public abstract class CsvTableEditorSwingTestBase extends BasePlatformTestCase {

    protected CsvTableEditorSwing fileEditor;

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/editor";
    }

    protected String getTestFile() { return "TableEditorFile.csv"; }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        initializeEditorSettings(CsvEditorSettings.getInstance());
        myFixture.configureByFiles(getTestFile());

        fileEditor = new CsvTableEditorSwing(this.getProject(), myFixture.getFile().getVirtualFile());
        fileEditor.selectNotify();
    }

    @Override
    protected void tearDown() throws Exception {
        fileEditor.dispose();

        super.tearDown();
    }

    protected void initializeEditorSettings(CsvEditorSettings instance) {
        instance.loadState(new CsvEditorSettings.OptionSet());
        instance.setFileEndLineBreak(false);
        instance.setTableAutoColumnWidthOnOpen(false);
    }

    @Deprecated
    protected Object[][] changeValue(String newValue, int row, int column) {
        fileEditor.getTable().setValueAt(newValue, row, column);
//        Object[][] copy = CsvHelper.deepCopy(initialState);
//        copy[row][column] = newValue;
        return new Object[0][0];
    }
}
