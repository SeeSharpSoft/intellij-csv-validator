package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.testFramework.exceptionCases.AbstractExceptionCase;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.ThrowableRunnable;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class CsvTableEditorSwingTestBase extends BasePlatformTestCase {

    protected CsvTableEditorSwing fileEditor;

    protected Object[][] initialState;

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
        initialState = fileEditor.getDataHandler().getCurrentState();
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

    protected Object[][] changeValue(String newValue, int row, int column) {
        fileEditor.getTable().setValueAt(newValue, row, column);
        Object[][] copy = CsvHelper.deepCopy(initialState);
        copy[row][column] = newValue;
        return copy;
    }


    public <T extends Throwable> void assertException(@NotNull final Class<? extends Throwable> exceptionClass, @Nullable String expectedErrorMsg, @NotNull final ThrowableRunnable<T> runnable) throws Throwable {
        assertException(new AbstractExceptionCase() {
            public Class<? extends Throwable> getExpectedExceptionClass() {
                return exceptionClass;
            }

            public void tryClosure() throws Throwable {
                runnable.run();
            }
        }, expectedErrorMsg);
    }
}
