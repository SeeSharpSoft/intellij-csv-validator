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
        originalTableEditorState.setFrozenColumnCount(2);

        originalTableEditorState.write(getProject(), domElement);

        CsvTableEditorState deserializeEditorState = CsvTableEditorState.create(domElement, this.getProject(), myFixture.getFile().getVirtualFile());

        assertOrderedEquals(deserializeEditorState.getColumnWidths(), originalTableEditorState.getColumnWidths());
        assertEquals(originalTableEditorState.getRowHeight(), deserializeEditorState.getRowHeight());
        assertEquals(2, deserializeEditorState.getFrozenColumnCount());
    }

    public void testOldStateDoesNotFreezeColumns() {
        CsvTableEditorState state = CsvTableEditorState.create(new Element("state"), getProject(), myFixture.getFile().getVirtualFile());
        assertEquals(0, state.getFrozenColumnCount());
    }

    public void testCopiedStatesCompareByLayout() {
        CsvTableEditorState state = new CsvTableEditorState();
        state.setColumnWidths(new int[]{80, 200});
        state.setRowHeight(42);
        state.setFrozenColumnCount(1);
        CsvTableEditorState copy = new CsvTableEditorState(state);
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
        state.getColumnWidths()[0] = 300;
        assertOrderedEquals(copy.getColumnWidths(), new int[]{80, 200});
        assertFalse(state.equals(copy));
    }

    public void testInvalidStoredFrozenColumnCountDoesNotFreezeColumns() {
        for (String value : new String[]{"-1", "invalid"}) {
            Element element = new Element("state").setAttribute("frozenColumnCount", value);
            assertEquals(0, CsvTableEditorState.create(element, getProject(), myFixture.getFile().getVirtualFile()).getFrozenColumnCount());
        }
        assertThrows(IllegalArgumentException.class, () -> new CsvTableEditorState().setFrozenColumnCount(-1));
    }
}
