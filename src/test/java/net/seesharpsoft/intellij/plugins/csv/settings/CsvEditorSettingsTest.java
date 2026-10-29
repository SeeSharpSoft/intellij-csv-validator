package net.seesharpsoft.intellij.plugins.csv.settings;

import com.intellij.util.xmlb.XmlSerializer;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;

import java.util.Objects;

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
        for (CsvEditorSettings.TableEditorType type : CsvEditorSettings.TableEditorType.values()) {
            assertNotNull(type.getDisplay());
        }
    }

    public void testTableEditorTypePersists() {
        CsvEditorSettings settings = new CsvEditorSettings();
        assertEquals(CsvEditorSettings.TableEditorType.FLATLAF, settings.getTableEditorType());
        settings.setTableEditorType(CsvEditorSettings.TableEditorType.SWING);

        CsvEditorSettings restored = new CsvEditorSettings();
        restored.loadState(XmlSerializer.deserialize(XmlSerializer.serialize(Objects.requireNonNull(settings.getState())), CsvEditorSettings.OptionSet.class));
        assertEquals(CsvEditorSettings.TableEditorType.SWING, restored.getTableEditorType());
    }

    public void testNullTableEditorTypeFallsBackToFlatLaf() {
        CsvEditorSettings settings = new CsvEditorSettings();
        Objects.requireNonNull(settings.getState()).TABLE_EDITOR_TYPE = null;
        assertEquals(CsvEditorSettings.TableEditorType.FLATLAF, settings.getTableEditorType());
        settings.setTableEditorType(null);
        assertEquals(CsvEditorSettings.TableEditorType.FLATLAF, settings.getState().TABLE_EDITOR_TYPE);
    }
}
