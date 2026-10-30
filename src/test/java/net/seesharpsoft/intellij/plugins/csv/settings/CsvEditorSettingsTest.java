package net.seesharpsoft.intellij.plugins.csv.settings;

import com.intellij.util.xmlb.XmlSerializer;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import org.jdom.Element;

public class CsvEditorSettingsTest extends CsvBasePlatformTestCase {

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/settings";
    }

    public void testDefaultValueSeparator() {
        myFixture.configureByFiles("AnyFile.csv");

        assertEquals(CsvEditorSettings.VALUE_SEPARATOR_DEFAULT, CsvHelper.getValueSeparator(myFixture.getFile()));
    }

    public void testDefaultEscapeCharacter() {
        myFixture.configureByFiles("AnyFile.csv");

        assertEquals(CsvEditorSettings.ESCAPE_CHARACTER_DEFAULT, CsvHelper.getEscapeCharacter(myFixture.getFile()));
    }

    public void testMaxHighlightingRowsRejectsNegativeValuesExceptUnlimited() {
        CsvEditorSettings settings = new CsvEditorSettings();
        assertEquals(-1, settings.getMaxHighlightingRows());
        for (int invalid : new int[]{-2, Integer.MIN_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> settings.setMaxHighlightingRows(invalid));
            CsvEditorSettings.OptionSet state = new CsvEditorSettings.OptionSet();
            state.MAX_HIGHLIGHTING_ROWS = invalid;
            assertThrows(IllegalArgumentException.class, () -> settings.loadState(state));
            assertEquals(-1, settings.getMaxHighlightingRows());
        }
        for (int valid : new int[]{0, 7, Integer.MAX_VALUE, -1}) {
            settings.setMaxHighlightingRows(valid);
            assertEquals(valid, settings.getMaxHighlightingRows());
        }
    }

    public void testGetDisplayDoesNotThrow() {
        for (CsvEditorSettings.EditorPrio prio : CsvEditorSettings.EditorPrio.values()) {
            assertNotNull(prio.getDisplay());
        }
        for (CsvEditorSettings.ValueColoring coloring : CsvEditorSettings.ValueColoring.values()) {
            assertNotNull(coloring.getDisplay());
        }
    }

    public void testLegacyEditorPreferenceIsIgnoredWithoutLosingTableSettings() {
        for (String legacyType : new String[]{"FLATLAF", "SWING"}) {
            Element state = new Element("CsvEditorSettings");
            state.addContent(new Element("option").setAttribute("name", "TABLE_EDITOR_TYPE").setAttribute("value", legacyType));
            state.addContent(new Element("option").setAttribute("name", "TABLE_EDITOR_ROW_HEIGHT").setAttribute("value", "36"));
            state.addContent(new Element("option").setAttribute("name", "TABLE_DEFAULT_COLUMN_WIDTH").setAttribute("value", "160"));

            CsvEditorSettings settings = new CsvEditorSettings();
            settings.loadState(XmlSerializer.deserialize(state, CsvEditorSettings.OptionSet.class));

            assertEquals(36, settings.getTableEditorRowHeight());
            assertEquals(160, settings.getTableDefaultColumnWidth());
            Element saved = XmlSerializer.serialize(settings.getState());
            assertFalse(saved.getChildren("option").stream()
                    .anyMatch(option -> "TABLE_EDITOR_TYPE".equals(option.getAttributeValue("name"))));
        }
    }
}
