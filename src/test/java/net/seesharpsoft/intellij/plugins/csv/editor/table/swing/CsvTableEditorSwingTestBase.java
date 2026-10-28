package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableModel;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.util.UpdateScheduler;

public abstract class CsvTableEditorSwingTestBase extends CsvBasePlatformTestCase {

    protected CsvTableEditorSwing fileEditor;
    UpdateScheduler updateScheduler;

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/editor";
    }

    protected String getTestFile() {
        return "TableEditorFile.csv";
    }

    protected int myInitialRowCount;
    protected int myInitialColumnCount;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        initializeEditorSettings(CsvEditorSettings.getInstance());
        myFixture.configureByFiles(getTestFile());

        fileEditor = createEditor();
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

    protected CsvTableEditorSwing createEditor() {
        if (updateScheduler == null) {
            return new CsvTableEditorSwing(getProject(), myFixture.getFile().getVirtualFile());
        }
        return new CsvTableEditorSwing(getProject(), myFixture.getFile().getVirtualFile()) {
            @Override
            protected CsvTableModelSwing createTableModel() {
                return createTableModelForTest(this);
            }
        };
    }

    protected CsvTableModelSwing createTableModelForTest(CsvTableEditorSwing editor) {
        return updateScheduler == null ? new CsvTableModelSwing(editor) : new CsvTableModelSwing(editor, updateScheduler);
    }

    protected void initializeEditorSettings(CsvEditorSettings instance) {
        instance.loadState(new CsvEditorSettings.OptionSet());
    }
}
