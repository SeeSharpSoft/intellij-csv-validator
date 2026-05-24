package net.seesharpsoft.intellij.plugins.csv.settings;

import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;

public class CsvEditorSettingsRowHeightTest extends CsvBasePlatformTestCase {

    public void testGetTableEditorRowHeightClamping() {
        CsvEditorSettings csvEditorSettings = CsvEditorSettings.getInstance();
        CsvEditorSettings.OptionSet optionSet = new CsvEditorSettings.OptionSet();
        
        // Setting an invalid low value
        optionSet.TABLE_EDITOR_ROW_HEIGHT = CsvEditorSettings.TABLE_EDITOR_ROW_HEIGHT_MIN - 1;
        csvEditorSettings.loadState(optionSet);
        
        assertEquals("Should return default height if stored value is too low", 
            CsvEditorSettings.TABLE_EDITOR_ROW_HEIGHT_DEFAULT, csvEditorSettings.getTableEditorRowHeight());

        // Setting an invalid high value
        optionSet.TABLE_EDITOR_ROW_HEIGHT = CsvEditorSettings.TABLE_EDITOR_ROW_HEIGHT_MAX + 1;
        csvEditorSettings.loadState(optionSet);
        
        assertEquals("Should return default height if stored value is too high", 
            CsvEditorSettings.TABLE_EDITOR_ROW_HEIGHT_DEFAULT, csvEditorSettings.getTableEditorRowHeight());

        // Setting a valid value
        int validHeight = CsvEditorSettings.TABLE_EDITOR_ROW_HEIGHT_MIN + 5;
        optionSet.TABLE_EDITOR_ROW_HEIGHT = validHeight;
        csvEditorSettings.loadState(optionSet);
        
        assertEquals("Should return the valid height", 
            validHeight, csvEditorSettings.getTableEditorRowHeight());
    }
}
