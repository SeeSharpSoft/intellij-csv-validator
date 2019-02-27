package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import net.seesharpsoft.intellij.plugins.csv.editor.CsvEditorSettingsExternalizable;
import org.jdom.Element;

public class CsvTableEditorStateTest extends LightCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/editor";
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CsvEditorSettingsExternalizable.getInstance().loadState(new CsvEditorSettingsExternalizable.OptionSet());
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

        CsvTableEditorState deserializeEditorState = CsvTableEditorState.create(domElement, this.getProject(), this.getFile().getVirtualFile());

        assertOrderedEquals(deserializeEditorState.getColumnWidths(), originalTableEditorState.getColumnWidths());
        assertEquals(originalTableEditorState.getRowLines(), deserializeEditorState.getRowLines());
        assertEquals(originalTableEditorState.getFixedHeaders(), deserializeEditorState.getFixedHeaders());
        assertEquals(originalTableEditorState.showInfoPanel(), deserializeEditorState.showInfoPanel());
    }
}
