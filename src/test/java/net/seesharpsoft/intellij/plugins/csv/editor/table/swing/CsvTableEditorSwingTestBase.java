package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableModel;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

public abstract class CsvTableEditorSwingTestBase extends BasePlatformTestCase {

    protected CsvTableEditorSwing fileEditor;

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/editor";
    }

    protected String getTestFile() { return "TableEditorFile.csv"; }

    protected int myInitialRowCount;
    protected int myInitialColumnCount;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        initializeEditorSettings(CsvEditorSettings.getInstance());
        myFixture.configureByFiles(getTestFile());

        fileEditor = new CsvTableEditorSwing(this.getProject(), myFixture.getFile().getVirtualFile());
        fileEditor.selectNotify();

        myInitialRowCount = getTableModel().getRowCount();
        myInitialColumnCount = getTableModel().getColumnCount();
    }

    @Override
    protected void tearDown() throws Exception {
        fileEditor.dispose();

        super.tearDown();
    }

    protected CsvTableModel getTableModel() {
        return fileEditor.getTableModel();
    }

    protected void initializeEditorSettings(CsvEditorSettings instance) {
        instance.loadState(new CsvEditorSettings.OptionSet());
        instance.setFileEndLineBreak(false);
    }
}
