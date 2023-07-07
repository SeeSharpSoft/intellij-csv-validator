package net.seesharpsoft.intellij.plugins.csv.editor.table;

import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.jdom.Element;

public class CsvTableEditorStateTest extends CsvBasePlatformTestCase {

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

        originalTableEditorState.setColumnWidths(new int[]{100, 50, 200, 42});
        originalTableEditorState.setRowHeight(42);

        originalTableEditorState.write(getProject(), domElement);

        CsvTableEditorState deserializeEditorState = CsvTableEditorState.create(domElement, this.getProject(), myFixture.getFile().getVirtualFile());

        assertOrderedEquals(deserializeEditorState.getColumnWidths(), originalTableEditorState.getColumnWidths());
        assertEquals(originalTableEditorState.getRowHeight(), deserializeEditorState.getRowHeight());
    }
}
