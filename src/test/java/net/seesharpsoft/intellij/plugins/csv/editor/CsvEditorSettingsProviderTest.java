package net.seesharpsoft.intellij.plugins.csv.editor;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;

import java.awt.*;

public class CsvEditorSettingsProviderTest extends LightCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/editor";
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testId() {
        CsvEditorSettingsProvider editorSettingsPanel = new CsvEditorSettingsProvider();

        assertEquals("Csv.Editor.Settings", editorSettingsPanel.getId());

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
        csvEditorSettingsExternalizable.setCaretRowShown(false);
        csvEditorSettingsExternalizable.setUseSoftWraps(true);
        csvEditorSettingsExternalizable.setColumnHighlightingEnabled(true);
        csvEditorSettingsExternalizable.setHighlightTabSeparator(false);
        csvEditorSettingsExternalizable.setTabHighlightColor(Color.BLACK);

        assertEquals(true, editorSettingsPanel.isModified());

        editorSettingsPanel.reset();

        assertEquals(false, editorSettingsPanel.isModified());
        assertEquals(false, csvEditorSettingsExternalizable.isCaretRowShown());
        assertEquals(true, csvEditorSettingsExternalizable.isUseSoftWraps());
        assertEquals(true, csvEditorSettingsExternalizable.isColumnHighlightingEnabled());
        assertEquals(false, csvEditorSettingsExternalizable.isHighlightTabSeparator());
        assertEquals(Color.BLACK, csvEditorSettingsExternalizable.getTabHighlightColor());

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
        csvEditorSettingsExternalizable.setTabHighlightColor(Color.BLACK);

        editorSettingsPanel.apply();

        CsvEditorSettingsExternalizable.OptionSet freshOptionSet = new CsvEditorSettingsExternalizable.OptionSet();

        assertEquals(false, editorSettingsPanel.isModified());
        assertEquals(freshOptionSet.CARET_ROW_SHOWN, csvEditorSettingsExternalizable.isCaretRowShown());
        assertEquals(freshOptionSet.USE_SOFT_WRAP, csvEditorSettingsExternalizable.isUseSoftWraps());
        assertEquals(freshOptionSet.COLUMN_HIGHTLIGHTING, csvEditorSettingsExternalizable.isColumnHighlightingEnabled());
        assertEquals(freshOptionSet.HIGHTLIGHT_TAB_SEPARATOR, csvEditorSettingsExternalizable.isHighlightTabSeparator());
        assertEquals(freshOptionSet.TAB_HIGHLIGHT_COLOR, "" + csvEditorSettingsExternalizable.getTabHighlightColor().getRGB());

        editorSettingsPanel.disposeUIResources();
    }

}
