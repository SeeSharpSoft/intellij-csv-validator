package net.seesharpsoft.intellij.plugins.csv.editor;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.ui.CheckBoxWithColorChooser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class CsvEditorSettingsProvider implements SearchableConfigurable {

    public static final String CSV_EDITOR_SETTINGS_ID = "Csv.Editor.Settings";

    private JCheckBox cbCaretRowShown;
    private JPanel myMainPanel;
    private JCheckBox cbUseSoftWraps;
    private JCheckBox cbColumnHighlighting;
    private CheckBoxWithColorChooser cbTabHighlightColor;
    private JCheckBox cbShowInfoBalloonCheckBox;
    private JCheckBox cbShowInfoPanel;
    private JComboBox cbRowHeight;
    private JComboBox cbEditorUsage;

    @NotNull
    @Override
    public String getId() {
        return CSV_EDITOR_SETTINGS_ID;
    }

    @Override
    public String getDisplayName() {
        return "CSV/TSV Editor";
    }

    @Override
    public String getHelpTopic() {
        return "Editor Options for CSV/TSV files";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return myMainPanel;
    }

    // ensure downward compatibility
    public boolean isModified(@NotNull JToggleButton toggleButton, boolean value) {
        return toggleButton.isSelected() != value;
    }

    @Override
    public boolean isModified() {
        CsvEditorSettingsExternalizable csvEditorSettingsExternalizable = CsvEditorSettingsExternalizable.getInstance();
        return isModified(cbCaretRowShown, csvEditorSettingsExternalizable.isCaretRowShown()) ||
                isModified(cbUseSoftWraps, csvEditorSettingsExternalizable.isUseSoftWraps()) ||
                isModified(cbColumnHighlighting, csvEditorSettingsExternalizable.isColumnHighlightingEnabled()) ||
                isModified(cbShowInfoBalloonCheckBox, csvEditorSettingsExternalizable.isShowInfoBalloon()) ||
                isModified(cbShowInfoPanel, csvEditorSettingsExternalizable.showTableEditorInfoPanel()) ||
                cbTabHighlightColor.isSelected() != csvEditorSettingsExternalizable.isHighlightTabSeparator() ||
                !Objects.equals(cbTabHighlightColor.getColor(), csvEditorSettingsExternalizable.getTabHighlightColor()) ||
                !Objects.equals(cbRowHeight.getSelectedIndex(), csvEditorSettingsExternalizable.getTableEditorRowHeight()) ||
                !Objects.equals(cbEditorUsage.getSelectedIndex(), csvEditorSettingsExternalizable.getEditorPrio().ordinal());
    }

    @Override
    public void reset() {
        CsvEditorSettingsExternalizable csvEditorSettingsExternalizable = CsvEditorSettingsExternalizable.getInstance();
        cbCaretRowShown.setSelected(csvEditorSettingsExternalizable.isCaretRowShown());
        cbUseSoftWraps.setSelected(csvEditorSettingsExternalizable.isUseSoftWraps());
        cbColumnHighlighting.setSelected(csvEditorSettingsExternalizable.isColumnHighlightingEnabled());
        cbShowInfoBalloonCheckBox.setSelected(csvEditorSettingsExternalizable.isShowInfoBalloon());
        cbShowInfoPanel.setSelected(csvEditorSettingsExternalizable.showTableEditorInfoPanel());
        cbTabHighlightColor.setSelected(csvEditorSettingsExternalizable.isHighlightTabSeparator());
        cbTabHighlightColor.setColor(csvEditorSettingsExternalizable.getTabHighlightColor());
        cbRowHeight.setSelectedIndex(csvEditorSettingsExternalizable.getTableEditorRowHeight());
        cbEditorUsage.setSelectedIndex(csvEditorSettingsExternalizable.getEditorPrio().ordinal());
    }

    @Override
    public void apply() throws ConfigurationException {
        CsvEditorSettingsExternalizable csvEditorSettingsExternalizable = CsvEditorSettingsExternalizable.getInstance();
        csvEditorSettingsExternalizable.setCaretRowShown(cbCaretRowShown.isSelected());
        csvEditorSettingsExternalizable.setUseSoftWraps(cbUseSoftWraps.isSelected());
        csvEditorSettingsExternalizable.setColumnHighlightingEnabled(cbColumnHighlighting.isSelected());
        csvEditorSettingsExternalizable.setShowInfoBalloon(cbShowInfoBalloonCheckBox.isSelected());
        csvEditorSettingsExternalizable.showTableEditorInfoPanel(cbShowInfoPanel.isSelected());
        csvEditorSettingsExternalizable.setHighlightTabSeparator(cbTabHighlightColor.isSelected());
        csvEditorSettingsExternalizable.setTabHighlightColor(cbTabHighlightColor.getColor());
        csvEditorSettingsExternalizable.setTableEditorRowHeight(cbRowHeight.getSelectedIndex());
        csvEditorSettingsExternalizable.setEditorPrio(CsvEditorSettingsExternalizable.EditorPrio.values()[cbEditorUsage.getSelectedIndex()]);
    }

    protected void createUIComponents() {
        cbTabHighlightColor = new CheckBoxWithColorChooser("Highlight tab separator   ");
        cbTabHighlightColor.setColor(Color.CYAN);
    }
}
