package net.seesharpsoft.intellij.plugins.csv.settings;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.CheckBoxWithColorChooser;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.xmlb.XmlSerializer;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.components.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.components.CsvValueSeparator;

import javax.swing.*;
import javax.swing.border.TitledBorder;
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

    public void testColumnHighlightingAppearsAfterTabSeparatorHighlighting() {
        CsvEditorSettingsProvider provider = new CsvEditorSettingsProvider();
        try {
            JComponent form = provider.createComponent();
            CheckBoxWithColorChooser tabHighlight = UIUtil.findComponentsOfType(form, CheckBoxWithColorChooser.class)
                    .stream().findFirst().orElseThrow();
            JCheckBox columnHighlight = UIUtil.findComponentsOfType(form, JCheckBox.class).stream()
                    .filter(component -> "Highlight active column".equals(component.getText()))
                    .findFirst().orElseThrow();
            Container highlighting = tabHighlight.getParent();
            Container columnOptions = columnHighlight.getParent();
            assertSame(highlighting, columnOptions.getParent());
            highlighting.setSize(highlighting.getPreferredSize());
            highlighting.doLayout();
            assertTrue("Column highlighting must appear below tab separator highlighting",
                    columnOptions.getY() >= tabHighlight.getY() + tabHighlight.getHeight());
        } finally {
            provider.disposeUIResources();
        }
    }

    public void testTextAndTableEditorSettingsExplainReopeningEditors() {
        CsvEditorSettingsProvider provider = new CsvEditorSettingsProvider();
        try {
            for (String title : new String[]{"Text Editor", "Table Editor"}) {
                JPanel group = UIUtil.findComponentsOfType(provider.createComponent(), JPanel.class).stream()
                        .filter(panel -> panel.getBorder() instanceof TitledBorder border && title.equals(border.getTitle()))
                        .findFirst().orElse(null);
                assertNotNull(title + " settings must be shown", group);
                JLabel notice = UIUtil.findComponentsOfType(group, JLabel.class).stream()
                        .filter(label -> "Changes do not apply to open editors. Close and reopen them to take effect."
                                .equals(label.getText()))
                        .findFirst().orElse(null);
                assertNotNull(title + " settings must explain when changes take effect", notice);
                assertTrue(title + " settings notice must be italic", notice.getFont().isItalic());
            }
        } finally {
            provider.disposeUIResources();
        }
    }

    public void testActiveColumnHighlightingDefaultsOnAndPersistsThroughSettingsForm() throws ConfigurationException {
        CsvEditorSettingsProvider provider = new CsvEditorSettingsProvider();
        try {
            provider.reset();
            JCheckBox checkbox = UIUtil.findComponentsOfType(provider.createComponent(), JCheckBox.class).stream()
                    .filter(component -> "Highlight active column".equals(component.getText()))
                    .findFirst().orElse(null);
            assertNotNull("Text editor highlighting must expose an active-column checkbox", checkbox);
            assertTrue(checkbox.isSelected());
            assertFalse(provider.isModified());

            checkbox.setSelected(false);
            assertTrue(provider.isModified());
            provider.apply();
            assertFalse(provider.isModified());

            checkbox.setSelected(true);
            assertTrue(provider.isModified());
            provider.reset();
            assertFalse(checkbox.isSelected());
            assertFalse(provider.isModified());

            CsvEditorSettings settings = CsvEditorSettings.getInstance();
            CsvEditorSettings.OptionSet savedState = XmlSerializer.deserialize(
                    XmlSerializer.serialize(settings.getState()), CsvEditorSettings.OptionSet.class);
            settings.loadState(new CsvEditorSettings.OptionSet());
            provider.reset();
            assertTrue(checkbox.isSelected());

            settings.loadState(savedState);
            provider.reset();
            assertFalse(checkbox.isSelected());
            checkbox.setSelected(true);
            provider.apply();
            provider.reset();
            assertTrue(checkbox.isSelected());
            assertFalse(provider.isModified());
        } finally {
            provider.disposeUIResources();
        }
    }

    public void testColumnHighlightingRowLimitDefaultsToUnlimitedAndPersistsThroughSettingsForm() throws ConfigurationException {
        CsvEditorSettingsProvider provider = new CsvEditorSettingsProvider();
        try {
            provider.reset();
            JComponent form = provider.createComponent();
            JLabel label = UIUtil.findComponentsOfType(form, JLabel.class).stream()
                    .filter(component -> "Maximum rows (-1 = no limit):".equals(component.getText()))
                    .findFirst().orElse(null);
            assertNotNull("Column highlighting must expose its row limit", label);
            assertTrue(label.getLabelFor() instanceof JFormattedTextField);
            JFormattedTextField limit = (JFormattedTextField) label.getLabelFor();
            assertEquals(-1, limit.getValue());
            assertTrue(limit.isEnabled());
            assertFalse(provider.isModified());

            JLabel notice = UIUtil.findComponentsOfType(form, JLabel.class).stream()
                    .filter(component -> "More highlighted rows can reduce editor responsiveness.".equals(component.getText()))
                    .findFirst().orElse(null);
            assertNotNull("The row limit must explain its responsiveness impact", notice);
            assertTrue(notice.getFont().isItalic());

            limit.setText("7");
            assertTrue(provider.isModified());
            provider.apply();
            assertFalse(provider.isModified());
            limit.setValue(15);
            provider.reset();
            assertEquals(7, limit.getValue());

            CsvEditorSettings settings = CsvEditorSettings.getInstance();
            CsvEditorSettings.OptionSet savedState = XmlSerializer.deserialize(
                    XmlSerializer.serialize(settings.getState()), CsvEditorSettings.OptionSet.class);
            settings.loadState(new CsvEditorSettings.OptionSet());
            provider.reset();
            assertEquals(-1, limit.getValue());
            settings.loadState(savedState);
            provider.reset();
            assertEquals(7, limit.getValue());

            JCheckBox checkbox = UIUtil.findComponentsOfType(form, JCheckBox.class).stream()
                    .filter(component -> "Highlight active column".equals(component.getText()))
                    .findFirst().orElseThrow();
            checkbox.setSelected(false);
            assertFalse(limit.isEnabled());
            assertFalse(label.isEnabled());
            checkbox.setSelected(true);
            assertTrue(limit.isEnabled());
            assertEquals(7, limit.getValue());
        } finally {
            provider.disposeUIResources();
        }
    }

    public void testColumnHighlightingRowLimitRejectsInvalidInputBeforeApplyingSettings() throws ConfigurationException {
        CsvEditorSettingsProvider provider = new CsvEditorSettingsProvider();
        try {
            JComponent form = provider.createComponent();
            JLabel label = UIUtil.findComponentsOfType(form, JLabel.class).stream()
                    .filter(component -> "Maximum rows (-1 = no limit):".equals(component.getText()))
                    .findFirst().orElseThrow();
            JFormattedTextField limit = (JFormattedTextField) label.getLabelFor();
            assertEquals(JFormattedTextField.PERSIST, limit.getFocusLostBehavior());
            JCheckBox checkbox = UIUtil.findComponentsOfType(form, JCheckBox.class).stream()
                    .filter(component -> "Highlight active column".equals(component.getText()))
                    .findFirst().orElseThrow();
            CsvEditorSettings settings = CsvEditorSettings.getInstance();

            for (String invalid : new String[]{"-2", "-1.5", "1.5", "-1E-3", "1E-3", "NaN",
                    "7rows", "", "text", "2147483648", "4294967296"}) {
                provider.reset();
                limit.setText(invalid);
                checkbox.setSelected(false);
                try {
                    provider.apply();
                    fail("Invalid row limit must be rejected: " + invalid);
                } catch (ConfigurationException exception) {
                    assertEquals("Maximum highlighted rows must be -1 (no limit) or a non-negative integer.",
                            exception.getMessage());
                }
                assertEquals(-1, settings.getMaxHighlightingRows());
                assertTrue("Validation must happen before changing other settings", settings.isHighlightActiveColumn());
            }

            provider.reset();
            for (int valid : new int[]{0, Integer.MAX_VALUE, -1}) {
                limit.setValue(valid);
                provider.apply();
                assertEquals(valid, settings.getMaxHighlightingRows());
                assertFalse(provider.isModified());
            }
        } finally {
            provider.disposeUIResources();
        }
    }

    public void testResetAndModified() {
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

            assertTrue(editorSettingsPanel.isModified());

            editorSettingsPanel.reset();

            assertFalse(editorSettingsPanel.isModified());

            assertFalse(csvEditorSettings.isCaretRowShown());
            assertTrue(csvEditorSettings.isUseSoftWraps());
            assertFalse(csvEditorSettings.isHighlightTabSeparator());
            assertFalse(csvEditorSettings.isShowInfoBalloon());
            assertEquals(Color.BLACK, csvEditorSettings.getTabHighlightColor());
            assertTrue(csvEditorSettings.isQuotingEnforced());
            assertTrue(csvEditorSettings.isZeroBasedColumnNumbering());
            assertEquals(500, csvEditorSettings.getTableDefaultColumnWidth());
            assertEquals(1000, csvEditorSettings.getTableAutoMaxColumnWidth());
            assertEquals(CsvEscapeCharacter.BACKSLASH, csvEditorSettings.getDefaultEscapeCharacter());
            assertEquals(CsvValueSeparator.PIPE, csvEditorSettings.getDefaultValueSeparator());
            assertTrue(csvEditorSettings.getKeepTrailingSpaces());
            assertEquals("//", csvEditorSettings.getCommentIndicator());
            assertEquals(CsvEditorSettings.ValueColoring.SIMPLE, csvEditorSettings.getValueColoring());
        } finally {
            editorSettingsPanel.disposeUIResources();
        }
    }

    public void testPixelRowHeightPersistsThroughSettingsForm() throws ConfigurationException {
        CsvEditorSettingsProvider provider = new CsvEditorSettingsProvider();
        try {
            provider.reset();
            JFormattedTextField height = getRowHeightField(provider);
            assertEquals(48, height.getValue());
            assertFalse(provider.isModified());

            height.setValue(24);
            assertTrue(provider.isModified());
            provider.apply();
            assertFalse(provider.isModified());
            provider.reset();
            assertEquals(24, height.getValue());

            CsvEditorSettings settings = CsvEditorSettings.getInstance();
            CsvEditorSettings.OptionSet saved = XmlSerializer.deserialize(
                    XmlSerializer.serialize(settings.getState()), CsvEditorSettings.OptionSet.class);
            settings.loadState(new CsvEditorSettings.OptionSet());
            settings.loadState(saved);
            provider.reset();
            assertEquals(24, height.getValue());
            assertEquals(24, settings.getTableEditorRowHeight());

            height.setValue(32);
            provider.apply();
            assertFalse(provider.isModified());
            provider.reset();
            assertEquals(32, height.getValue());
        } finally {
            provider.disposeUIResources();
        }
    }

    private JFormattedTextField getRowHeightField(CsvEditorSettingsProvider provider) {
        JLabel label = UIUtil.findComponentsOfType(provider.createComponent(), JLabel.class).stream()
                .filter(component -> component.getText() != null && component.getText().startsWith("Default row height"))
                .findFirst().orElseThrow();
        return (JFormattedTextField) label.getLabelFor();
    }

    public void testApply() throws ConfigurationException {
        CsvEditorSettingsProvider editorSettingsPanel = new CsvEditorSettingsProvider();
        try {
            CsvEditorSettings csvEditorSettings = CsvEditorSettings.getInstance();
            csvEditorSettings.loadState(new CsvEditorSettings.OptionSet());
            editorSettingsPanel.reset();

            assertFalse(editorSettingsPanel.isModified());

            // 1. Change settings and verify isModified() becomes true
            csvEditorSettings.setCaretRowShown(!csvEditorSettings.isCaretRowShown());
            assertTrue(editorSettingsPanel.isModified());

            // 2. Reset and verify isModified() becomes false and settings are back to UI state
            csvEditorSettings.setCaretRowShown(!csvEditorSettings.isCaretRowShown());
            assertFalse(editorSettingsPanel.isModified());
            
            // Re-initialize for a clean start
            csvEditorSettings.loadState(new CsvEditorSettings.OptionSet());
            editorSettingsPanel.reset();
            
            // Manually change a setting in CsvEditorSettings
            boolean originalValue = csvEditorSettings.isCaretRowShown();
            csvEditorSettings.setCaretRowShown(!originalValue);
            
            assertTrue(editorSettingsPanel.isModified());
            
            // apply() should write the UI state (originalValue) back to csvEditorSettings
            editorSettingsPanel.apply();
            
            assertFalse(editorSettingsPanel.isModified());
            assertEquals(originalValue, csvEditorSettings.isCaretRowShown());

        } finally {
            editorSettingsPanel.disposeUIResources();
        }
    }

    public void testTableSettingsHaveNoImplementationSelector() {
        CsvEditorSettingsProvider provider = new CsvEditorSettingsProvider();
        try {
            JPanel tableSettings = UIUtil.findComponentsOfType(provider.createComponent(), JPanel.class).stream()
                    .filter(panel -> panel.getBorder() instanceof TitledBorder border && "Table Editor".equals(border.getTitle()))
                    .findFirst().orElseThrow();
            assertTrue(UIUtil.findComponentsOfType(tableSettings, JComboBox.class).isEmpty());
            assertEquals(3, UIUtil.findComponentsOfType(tableSettings, JFormattedTextField.class).size());
        } finally {
            provider.disposeUIResources();
        }
    }

}
