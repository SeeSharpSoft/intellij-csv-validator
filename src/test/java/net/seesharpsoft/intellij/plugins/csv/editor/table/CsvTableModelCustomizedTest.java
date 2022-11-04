package net.seesharpsoft.intellij.plugins.csv.editor.table;

import net.seesharpsoft.intellij.plugins.csv.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.CsvValueSeparator;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

public class CsvTableModelCustomizedTest extends CsvTableModelBaseTest {
    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/editor/table/customized";
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CsvEditorSettings.getInstance().setDefaultValueSeparator(new CsvValueSeparator("$~"));
        CsvEditorSettings.getInstance().setDefaultEscapeCharacter(CsvEscapeCharacter.BACKSLASH);
        CsvEditorSettings.getInstance().setCommentIndicator("///");
    }

    @Override
    public void testGetValue() {
        manualCheck(csvTableModel -> {
            assertEquals("Header 1", csvTableModel.getValue(0, 0));
            assertEquals("", csvTableModel.getValue(100, 100));
            assertEquals("Header 2", csvTableModel.getValue(0, 1));
            assertEquals("  Value 3 ", csvTableModel.getValue(1, 2));
            assertEquals(" Value 4", csvTableModel.getValue(1, 3));
            assertEquals("", csvTableModel.getValue(2, 1));
            assertEquals(" I am a comment $~;|\" nothing happens $~$~$~$~$~$~$~$~$~$~$~", csvTableModel.getValue(5, 3));
            assertEquals("#not a comment", csvTableModel.getValue(7, 4));
            assertEquals(";:|\n\tvalue 2", csvTableModel.getValue(8, 3));
            assertEquals("\\\\\\\\end", csvTableModel.getValue(8, 5));
        });
    }
}
