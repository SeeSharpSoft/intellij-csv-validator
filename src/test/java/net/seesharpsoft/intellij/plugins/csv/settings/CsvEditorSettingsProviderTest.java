package net.seesharpsoft.intellij.plugins.csv.settings;

import com.intellij.openapi.options.ConfigurationException;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.components.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.components.CsvValueSeparator;

import java.awt.*;

public class CsvEditorSettingsProviderTest extends CsvBasePlatformTestCase {

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/editor";
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CsvEditorSettings.getInstance().loadState(new CsvEditorSettings.OptionSet());
    }

    @Override
    protected void tearDown() throws Exception {
        CsvEditorSettings.getInstance().loadState(new CsvEditorSettings.OptionSet());
        super.tearDown();
    }

    public void testId() {
        CsvEditorSettingsProvider editorSettingsPanel = new CsvEditorSettingsProvider();
        try {
            assertEquals(CsvEditorSettingsProvider.CSV_EDITOR_SETTINGS_ID, editorSettingsPanel.getId());
        } finally {
            editorSettingsPanel.disposeUIResources();
        }
    }

    public void testDisplayName() {
        CsvEditorSettingsProvider editorSettingsPanel = new CsvEditorSettingsProvider();
        try {
            assertEquals("CSV/TSV/PSV", editorSettingsPanel.getDisplayName());
        } finally {
            editorSettingsPanel.disposeUIResources();
        }
    }

    public void testHelpTopic() {
        CsvEditorSettingsProvider editorSettingsPanel = new CsvEditorSettingsProvider();
        try {
            assertEquals("Editor Options for CSV/TSV/PSV files", editorSettingsPanel.getHelpTopic());
        } finally {
            editorSettingsPanel.disposeUIResources();
        }
    }

    public void testComponent() {
        CsvEditorSettingsProvider editorSettingsPanel = new CsvEditorSettingsProvider();
        try {
            assertNotNull(editorSettingsPanel.createComponent());
        } finally {
            editorSettingsPanel.disposeUIResources();
        }
    }

    public void testResetAndModified() throws ConfigurationException {
        CsvEditorSettingsProvider editorSettingsPanel = new CsvEditorSettingsProvider();
        try {
            CsvEditorSettings csvEditorSettings = CsvEditorSettings.getInstance();
            csvEditorSettings.loadState(new CsvEditorSettings.OptionSet());
            csvEditorSettings.setCaretRowShown(false);
            csvEditorSettings.setUseSoftWraps(true);
            csvEditorSettings.setHighlightTabSeparator(false);
            csvEditorSettings.setShowInfoBalloon(false);
            csvEditorSettings.setTabHighlightColor(Color.BLACK);
            csvEditorSettings.setQuotingEnforced(true);
            csvEditorSettings.setZeroBasedColumnNumbering(true);
            csvEditorSettings.setTableDefaultColumnWidth(500);
            csvEditorSettings.setTableAutoMaxColumnWidth(1000);
            csvEditorSettings.setDefaultEscapeCharacter(CsvEscapeCharacter.BACKSLASH);
            csvEditorSettings.setDefaultValueSeparator(CsvValueSeparator.PIPE);
            csvEditorSettings.setKeepTrailingSpaces(true);
            csvEditorSettings.setCommentIndicator("//");
            csvEditorSettings.setValueColoring(CsvEditorSettings.ValueColoring.SIMPLE);

            assertEquals(true, editorSettingsPanel.isModified());

            editorSettingsPanel.reset();

            assertEquals(false, editorSettingsPanel.isModified());

            assertEquals(false, csvEditorSettings.isCaretRowShown());
            assertEquals(true, csvEditorSettings.isUseSoftWraps());
            assertEquals(false, csvEditorSettings.isHighlightTabSeparator());
            assertEquals(false, csvEditorSettings.isShowInfoBalloon());
            assertEquals(Color.BLACK, csvEditorSettings.getTabHighlightColor());
            assertEquals(true, csvEditorSettings.isQuotingEnforced());
            assertEquals(true, csvEditorSettings.isZeroBasedColumnNumbering());
            assertEquals(500, csvEditorSettings.getTableDefaultColumnWidth());
            assertEquals(1000, csvEditorSettings.getTableAutoMaxColumnWidth());
            assertEquals(CsvEscapeCharacter.BACKSLASH, csvEditorSettings.getDefaultEscapeCharacter());
            assertEquals(CsvValueSeparator.PIPE, csvEditorSettings.getDefaultValueSeparator());
            assertEquals(true, csvEditorSettings.getKeepTrailingSpaces());
            assertEquals("//", csvEditorSettings.getCommentIndicator());
            assertEquals(CsvEditorSettings.ValueColoring.SIMPLE, csvEditorSettings.getValueColoring());
        } finally {
            editorSettingsPanel.disposeUIResources();
        }
    }

    public void testApply() throws ConfigurationException {
        CsvEditorSettingsProvider editorSettingsPanel = new CsvEditorSettingsProvider();
        try {
            CsvEditorSettings csvEditorSettings = CsvEditorSettings.getInstance();
            csvEditorSettings.loadState(new CsvEditorSettings.OptionSet());
            editorSettingsPanel.reset();

            assertEquals(false, editorSettingsPanel.isModified());

            // 1. Change settings and verify isModified() becomes true
            csvEditorSettings.setCaretRowShown(!csvEditorSettings.isCaretRowShown());
            assertEquals(true, editorSettingsPanel.isModified());

            // 2. Reset and verify isModified() becomes false and settings are back to UI state
            csvEditorSettings.setCaretRowShown(!csvEditorSettings.isCaretRowShown());
            assertEquals(false, editorSettingsPanel.isModified());
            
            // Re-initialize for a clean start
            csvEditorSettings.loadState(new CsvEditorSettings.OptionSet());
            editorSettingsPanel.reset();
            
            // Manually change a setting in CsvEditorSettings
            boolean originalValue = csvEditorSettings.isCaretRowShown();
            csvEditorSettings.setCaretRowShown(!originalValue);
            
            assertEquals(true, editorSettingsPanel.isModified());
            
            // apply() should write the UI state (originalValue) back to csvEditorSettings
            editorSettingsPanel.apply();
            
            assertEquals(false, editorSettingsPanel.isModified());
            assertEquals(originalValue, csvEditorSettings.isCaretRowShown());

        } finally {
            editorSettingsPanel.disposeUIResources();
        }
    }

}
