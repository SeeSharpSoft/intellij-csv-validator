package net.seesharpsoft.intellij.plugins.csv.editor;

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
        CsvEditorSettingsExternalizable.getInstance().loadState(new CsvEditorSettingsExternalizable.OptionSet());
    }

    public void testId() {
        CsvEditorSettingsProvider editorSettingsPanel = new CsvEditorSettingsProvider();

        assertEquals(CsvEditorSettingsProvider.CSV_EDITOR_SETTINGS_ID, editorSettingsPanel.getId());

        editorSettingsPanel.disposeUIResources();
    }

    public void testDisplayName() {
        CsvEditorSettingsProvider editorSettingsPanel = new CsvEditorSettingsProvider();

        assertEquals("CSV/TSV Editor", editorSettingsPanel.getDisplayName());

        editorSettingsPanel.disposeUIResources();
    }

    public void testHelpTopic() {
        CsvEditorSettingsProvider editorSettingsPanel = new CsvEditorSettingsProvider();

        assertEquals("Editor Options for CSV/TSV files", editorSettingsPanel.getHelpTopic());

        editorSettingsPanel.disposeUIResources();
    }

    public void testComponent() {
        CsvEditorSettingsProvider editorSettingsPanel = new CsvEditorSettingsProvider();

        assertNotNull(editorSettingsPanel.createComponent());

        editorSettingsPanel.disposeUIResources();
    }

    public void testResetAndModified() throws ConfigurationException {
        CsvEditorSettingsProvider editorSettingsPanel = new CsvEditorSettingsProvider();

        CsvEditorSettingsExternalizable csvEditorSettingsExternalizable = CsvEditorSettingsExternalizable.getInstance();
        csvEditorSettingsExternalizable.loadState(new CsvEditorSettingsExternalizable.OptionSet());
        csvEditorSettingsExternalizable.setCaretRowShown(false);
        csvEditorSettingsExternalizable.setUseSoftWraps(true);
        csvEditorSettingsExternalizable.setColumnHighlightingEnabled(true);
        csvEditorSettingsExternalizable.setHighlightTabSeparator(false);
        csvEditorSettingsExternalizable.setShowInfoBalloon(false);
        csvEditorSettingsExternalizable.setTabHighlightColor(Color.BLACK);
        csvEditorSettingsExternalizable.setQuotingEnforced(true);
        csvEditorSettingsExternalizable.setTableColumnHighlightingEnabled(false);
        csvEditorSettingsExternalizable.setZeroBasedColumnNumbering(true);
        csvEditorSettingsExternalizable.setFileEndLineBreak(false);
        csvEditorSettingsExternalizable.setTableDefaultColumnWidth(500);
        csvEditorSettingsExternalizable.setTableAutoMaxColumnWidth(1000);
        csvEditorSettingsExternalizable.setTableAutoColumnWidthOnOpen(false);

        assertEquals(true, editorSettingsPanel.isModified());

        editorSettingsPanel.reset();

        assertEquals(false, editorSettingsPanel.isModified());
        assertEquals(false, csvEditorSettingsExternalizable.isCaretRowShown());
        assertEquals(true, csvEditorSettingsExternalizable.isUseSoftWraps());
        assertEquals(true, csvEditorSettingsExternalizable.isColumnHighlightingEnabled());
        assertEquals(false, csvEditorSettingsExternalizable.isHighlightTabSeparator());
        assertEquals(false, csvEditorSettingsExternalizable.isShowInfoBalloon());
        assertEquals(Color.BLACK, csvEditorSettingsExternalizable.getTabHighlightColor());
        assertEquals(true, csvEditorSettingsExternalizable.isQuotingEnforced());
        assertEquals(false, csvEditorSettingsExternalizable.isTableColumnHighlightingEnabled());
        assertEquals(true, csvEditorSettingsExternalizable.isZeroBasedColumnNumbering());
        assertEquals(false, csvEditorSettingsExternalizable.isFileEndLineBreak());
        assertEquals(500, csvEditorSettingsExternalizable.getTableDefaultColumnWidth());
        assertEquals(1000, csvEditorSettingsExternalizable.getTableAutoMaxColumnWidth());
        assertEquals(false, csvEditorSettingsExternalizable.isTableAutoColumnWidthOnOpen());

        editorSettingsPanel.disposeUIResources();
    }

    public void testApply() throws ConfigurationException {
        CsvEditorSettingsProvider editorSettingsPanel = new CsvEditorSettingsProvider();

        CsvEditorSettingsExternalizable csvEditorSettingsExternalizable = CsvEditorSettingsExternalizable.getInstance();
        csvEditorSettingsExternalizable.loadState(new CsvEditorSettingsExternalizable.OptionSet());
        editorSettingsPanel.reset();
        csvEditorSettingsExternalizable.setCaretRowShown(false);
        csvEditorSettingsExternalizable.setUseSoftWraps(true);
        csvEditorSettingsExternalizable.setColumnHighlightingEnabled(true);
        csvEditorSettingsExternalizable.setHighlightTabSeparator(false);
        csvEditorSettingsExternalizable.setShowInfoBalloon(false);
        csvEditorSettingsExternalizable.setTabHighlightColor(Color.BLACK);
        csvEditorSettingsExternalizable.setQuotingEnforced(true);
        csvEditorSettingsExternalizable.setTableColumnHighlightingEnabled(false);
        csvEditorSettingsExternalizable.setZeroBasedColumnNumbering(true);
        csvEditorSettingsExternalizable.setFileEndLineBreak(false);
        csvEditorSettingsExternalizable.setTableDefaultColumnWidth(500);
        csvEditorSettingsExternalizable.setTableAutoMaxColumnWidth(1000);
        csvEditorSettingsExternalizable.setTableAutoColumnWidthOnOpen(false);

        editorSettingsPanel.apply();

        CsvEditorSettingsExternalizable.OptionSet freshOptionSet = new CsvEditorSettingsExternalizable.OptionSet();

        assertEquals(false, editorSettingsPanel.isModified());
        assertEquals(freshOptionSet.CARET_ROW_SHOWN, csvEditorSettingsExternalizable.isCaretRowShown());
        assertEquals(freshOptionSet.USE_SOFT_WRAP, csvEditorSettingsExternalizable.isUseSoftWraps());
        assertEquals(freshOptionSet.COLUMN_HIGHTLIGHTING, csvEditorSettingsExternalizable.isColumnHighlightingEnabled());
        assertEquals(freshOptionSet.HIGHTLIGHT_TAB_SEPARATOR, csvEditorSettingsExternalizable.isHighlightTabSeparator());
        assertEquals(freshOptionSet.SHOW_INFO_BALLOON, csvEditorSettingsExternalizable.isShowInfoBalloon());
        assertEquals(freshOptionSet.TAB_HIGHLIGHT_COLOR, "" + csvEditorSettingsExternalizable.getTabHighlightColor().getRGB());
        assertEquals(freshOptionSet.QUOTING_ENFORCED, csvEditorSettingsExternalizable.isQuotingEnforced());
        assertEquals(freshOptionSet.TABLE_COLUMN_HIGHTLIGHTING, csvEditorSettingsExternalizable.isTableColumnHighlightingEnabled());
        assertEquals(freshOptionSet.ZERO_BASED_COLUMN_NUMBERING, csvEditorSettingsExternalizable.isZeroBasedColumnNumbering());
        assertEquals(freshOptionSet.FILE_END_LINE_BREAK, csvEditorSettingsExternalizable.isFileEndLineBreak());
        assertEquals(freshOptionSet.TABLE_DEFAULT_COLUMN_WIDTH, csvEditorSettingsExternalizable.getTableDefaultColumnWidth());
        assertEquals(freshOptionSet.TABLE_AUTO_MAX_COLUMN_WIDTH, csvEditorSettingsExternalizable.getTableAutoMaxColumnWidth());
        assertEquals(freshOptionSet.TABLE_AUTO_COLUMN_WIDTH_ON_OPEN, csvEditorSettingsExternalizable.isTableAutoColumnWidthOnOpen());

        editorSettingsPanel.disposeUIResources();
    }

}
