package net.seesharpsoft.intellij.plugins.csv.settings;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;

import java.awt.*;

public class CsvEditorSettingsProviderTest extends LightPlatformCodeInsightFixtureTestCase {

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

        assertEquals(CsvEditorSettingsProvider.CSV_EDITOR_SETTINGS_ID, editorSettingsPanel.getId());

        editorSettingsPanel.disposeUIResources();
    }

    public void testDisplayName() {
        CsvEditorSettingsProvider editorSettingsPanel = new CsvEditorSettingsProvider();

        assertEquals("CSV/TSV/PSV", editorSettingsPanel.getDisplayName());

        editorSettingsPanel.disposeUIResources();
    }

    public void testHelpTopic() {
        CsvEditorSettingsProvider editorSettingsPanel = new CsvEditorSettingsProvider();

        assertEquals("Editor Options for CSV/TSV/PSV files", editorSettingsPanel.getHelpTopic());

        editorSettingsPanel.disposeUIResources();
    }

    public void testComponent() {
        CsvEditorSettingsProvider editorSettingsPanel = new CsvEditorSettingsProvider();

        assertNotNull(editorSettingsPanel.createComponent());

        editorSettingsPanel.disposeUIResources();
    }

    public void testResetAndModified() throws ConfigurationException {
        CsvEditorSettingsProvider editorSettingsPanel = new CsvEditorSettingsProvider();

        CsvEditorSettings csvEditorSettings = CsvEditorSettings.getInstance();
        csvEditorSettings.loadState(new CsvEditorSettings.OptionSet());
        csvEditorSettings.setCaretRowShown(false);
        csvEditorSettings.setUseSoftWraps(true);
        csvEditorSettings.setColumnHighlightingEnabled(true);
        csvEditorSettings.setHighlightTabSeparator(false);
        csvEditorSettings.setShowInfoBalloon(false);
        csvEditorSettings.setTabHighlightColor(Color.BLACK);
        csvEditorSettings.setQuotingEnforced(true);
        csvEditorSettings.setTableColumnHighlightingEnabled(false);
        csvEditorSettings.setZeroBasedColumnNumbering(true);
        csvEditorSettings.setFileEndLineBreak(false);
        csvEditorSettings.setTableDefaultColumnWidth(500);
        csvEditorSettings.setTableAutoMaxColumnWidth(1000);
        csvEditorSettings.setTableAutoColumnWidthOnOpen(false);

        assertEquals(true, editorSettingsPanel.isModified());

        editorSettingsPanel.reset();

        assertEquals(false, editorSettingsPanel.isModified());
        assertEquals(false, csvEditorSettings.isCaretRowShown());
        assertEquals(true, csvEditorSettings.isUseSoftWraps());
        assertEquals(true, csvEditorSettings.isColumnHighlightingEnabled());
        assertEquals(false, csvEditorSettings.isHighlightTabSeparator());
        assertEquals(false, csvEditorSettings.isShowInfoBalloon());
        assertEquals(Color.BLACK, csvEditorSettings.getTabHighlightColor());
        assertEquals(true, csvEditorSettings.isQuotingEnforced());
        assertEquals(false, csvEditorSettings.isTableColumnHighlightingEnabled());
        assertEquals(true, csvEditorSettings.isZeroBasedColumnNumbering());
        assertEquals(false, csvEditorSettings.isFileEndLineBreak());
        assertEquals(500, csvEditorSettings.getTableDefaultColumnWidth());
        assertEquals(1000, csvEditorSettings.getTableAutoMaxColumnWidth());
        assertEquals(false, csvEditorSettings.isTableAutoColumnWidthOnOpen());

        editorSettingsPanel.disposeUIResources();
    }

    public void testApply() throws ConfigurationException {
        CsvEditorSettingsProvider editorSettingsPanel = new CsvEditorSettingsProvider();

        CsvEditorSettings csvEditorSettings = CsvEditorSettings.getInstance();
        csvEditorSettings.loadState(new CsvEditorSettings.OptionSet());
        editorSettingsPanel.reset();
        csvEditorSettings.setCaretRowShown(false);
        csvEditorSettings.setUseSoftWraps(true);
        csvEditorSettings.setColumnHighlightingEnabled(true);
        csvEditorSettings.setHighlightTabSeparator(false);
        csvEditorSettings.setShowInfoBalloon(false);
        csvEditorSettings.setTabHighlightColor(Color.BLACK);
        csvEditorSettings.setQuotingEnforced(true);
        csvEditorSettings.setTableColumnHighlightingEnabled(false);
        csvEditorSettings.setZeroBasedColumnNumbering(true);
        csvEditorSettings.setFileEndLineBreak(false);
        csvEditorSettings.setTableDefaultColumnWidth(500);
        csvEditorSettings.setTableAutoMaxColumnWidth(1000);
        csvEditorSettings.setTableAutoColumnWidthOnOpen(false);

        editorSettingsPanel.apply();

        CsvEditorSettings.OptionSet freshOptionSet = new CsvEditorSettings.OptionSet();

        assertEquals(false, editorSettingsPanel.isModified());
        assertEquals(freshOptionSet.CARET_ROW_SHOWN, csvEditorSettings.isCaretRowShown());
        assertEquals(freshOptionSet.USE_SOFT_WRAP, csvEditorSettings.isUseSoftWraps());
        assertEquals(freshOptionSet.COLUMN_HIGHTLIGHTING, csvEditorSettings.isColumnHighlightingEnabled());
        assertEquals(freshOptionSet.HIGHTLIGHT_TAB_SEPARATOR, csvEditorSettings.isHighlightTabSeparator());
        assertEquals(freshOptionSet.SHOW_INFO_BALLOON, csvEditorSettings.isShowInfoBalloon());
        assertEquals(freshOptionSet.TAB_HIGHLIGHT_COLOR, "" + csvEditorSettings.getTabHighlightColor().getRGB());
        assertEquals(freshOptionSet.QUOTING_ENFORCED, csvEditorSettings.isQuotingEnforced());
        assertEquals(freshOptionSet.TABLE_COLUMN_HIGHTLIGHTING, csvEditorSettings.isTableColumnHighlightingEnabled());
        assertEquals(freshOptionSet.ZERO_BASED_COLUMN_NUMBERING, csvEditorSettings.isZeroBasedColumnNumbering());
        assertEquals(freshOptionSet.FILE_END_LINE_BREAK, csvEditorSettings.isFileEndLineBreak());
        assertEquals(freshOptionSet.TABLE_DEFAULT_COLUMN_WIDTH, csvEditorSettings.getTableDefaultColumnWidth());
        assertEquals(freshOptionSet.TABLE_AUTO_MAX_COLUMN_WIDTH, csvEditorSettings.getTableAutoMaxColumnWidth());
        assertEquals(freshOptionSet.TABLE_AUTO_COLUMN_WIDTH_ON_OPEN, csvEditorSettings.isTableAutoColumnWidthOnOpen());

        editorSettingsPanel.disposeUIResources();
    }

}
