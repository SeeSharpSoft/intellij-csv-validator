package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.jdom.Element;

public class CsvTableEditorStateTest extends BasePlatformTestCase {

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/editor";
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CsvEditorSettings.getInstance().loadState(new CsvEditorSettings.OptionSet());
        myFixture.configureByFiles("AnyFile.csv");
    }

    public void testCsvTableEditorStateSerializationAndDeserialization() {
        Element domElement = new Element("dummy");
        CsvTableEditorState originalTableEditorState = new CsvTableEditorState();

        originalTableEditorState.setColumnWidths(new int[] { 100, 50, 200, 42});
        originalTableEditorState.setRowLines(42);
        originalTableEditorState.setFixedHeaders(!originalTableEditorState.getFixedHeaders());
        originalTableEditorState.setShowInfoPanel(!originalTableEditorState.showInfoPanel());

        originalTableEditorState.write(getProject(), domElement);

        CsvTableEditorState deserializeEditorState = CsvTableEditorState.create(domElement, this.getProject(), myFixture.getFile().getVirtualFile());

        assertOrderedEquals(deserializeEditorState.getColumnWidths(), originalTableEditorState.getColumnWidths());
        assertEquals(originalTableEditorState.getRowLines(), deserializeEditorState.getRowLines());
        assertEquals(originalTableEditorState.getFixedHeaders(), deserializeEditorState.getFixedHeaders());
        assertEquals(originalTableEditorState.showInfoPanel(), deserializeEditorState.showInfoPanel());
    }
}
