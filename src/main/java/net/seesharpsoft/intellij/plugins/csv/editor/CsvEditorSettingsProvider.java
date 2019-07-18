package net.seesharpsoft.intellij.plugins.csv.editor;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.ui.CheckBoxWithColorChooser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Objects;

public class CsvEditorSettingsProvider implements SearchableConfigurable {

    public static final String CSV_EDITOR_SETTINGS_ID = "Csv.Editor.Settings";

    public static final int MIN_TABLE_COLUMN_SIZE = 10;
    public static final int MAX_TABLE_COLUMN_SIZE = 10000;

    private JCheckBox cbCaretRowShown;
    private JPanel myMainPanel;
    private JCheckBox cbUseSoftWraps;
    private JCheckBox cbColumnHighlighting;
    private CheckBoxWithColorChooser cbTabHighlightColor;
    private JCheckBox cbShowInfoBalloonCheckBox;
    private JCheckBox cbShowInfoPanel;
    private JComboBox cbRowHeight;
    private JComboBox cbEditorUsage;
    private JCheckBox cbQuotingEnforced;
    private JCheckBox cbTableColumnHighlighting;
    private JCheckBox cbZeroBasedColumnNumbering;
    private JCheckBox cbFileEndLineBreak;
    private JFormattedTextField tfMaxColumnWidth;
    private JFormattedTextField tfDefaultColumnWidth;
    private JCheckBox cbAdjustColumnWidthOnOpen;

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
                !Objects.equals(cbEditorUsage.getSelectedIndex(), csvEditorSettingsExternalizable.getEditorPrio().ordinal()) ||
                isModified(cbQuotingEnforced, csvEditorSettingsExternalizable.isQuotingEnforced()) ||
                !Objects.equals(cbEditorUsage.getSelectedIndex(), csvEditorSettingsExternalizable.getEditorPrio().ordinal()) ||
                isModified(cbTableColumnHighlighting, csvEditorSettingsExternalizable.isTableColumnHighlightingEnabled()) ||
                isModified(cbZeroBasedColumnNumbering, csvEditorSettingsExternalizable.isZeroBasedColumnNumbering()) ||
                isModified(cbFileEndLineBreak, csvEditorSettingsExternalizable.isFileEndLineBreak()) ||
                !tfMaxColumnWidth.getValue().equals(csvEditorSettingsExternalizable.getTableAutoMaxColumnWidth()) ||
                !tfDefaultColumnWidth.getValue().equals(csvEditorSettingsExternalizable.getTableDefaultColumnWidth()) ||
                isModified(cbAdjustColumnWidthOnOpen, csvEditorSettingsExternalizable.isTableAutoColumnWidthOnOpen());
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
        cbQuotingEnforced.setSelected(csvEditorSettingsExternalizable.isQuotingEnforced());
        cbTableColumnHighlighting.setSelected(csvEditorSettingsExternalizable.isTableColumnHighlightingEnabled());
        cbZeroBasedColumnNumbering.setSelected(csvEditorSettingsExternalizable.isZeroBasedColumnNumbering());
        cbFileEndLineBreak.setSelected(csvEditorSettingsExternalizable.isFileEndLineBreak());
        tfMaxColumnWidth.setValue(csvEditorSettingsExternalizable.getTableAutoMaxColumnWidth());
        tfDefaultColumnWidth.setValue(csvEditorSettingsExternalizable.getTableDefaultColumnWidth());
        cbAdjustColumnWidthOnOpen.setSelected(csvEditorSettingsExternalizable.isTableAutoColumnWidthOnOpen());
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
        csvEditorSettingsExternalizable.setQuotingEnforced(cbQuotingEnforced.isSelected());
        csvEditorSettingsExternalizable.setTableColumnHighlightingEnabled(cbTableColumnHighlighting.isSelected());
        csvEditorSettingsExternalizable.setZeroBasedColumnNumbering(cbZeroBasedColumnNumbering.isSelected());
        csvEditorSettingsExternalizable.setFileEndLineBreak(cbFileEndLineBreak.isSelected());
        csvEditorSettingsExternalizable.setTableAutoMaxColumnWidth((int)tfMaxColumnWidth.getValue());
        csvEditorSettingsExternalizable.setTableDefaultColumnWidth((int)tfDefaultColumnWidth.getValue());
        csvEditorSettingsExternalizable.setTableAutoColumnWidthOnOpen(cbAdjustColumnWidthOnOpen.isSelected());
    }

    protected void createUIComponents() {
        cbTabHighlightColor = new CheckBoxWithColorChooser("Highlight tab separator   ");
        cbTabHighlightColor.setColor(Color.CYAN);

        NumberFormat numberFormat = NumberFormat.getIntegerInstance();
        NumberFormatter numberFormatter = new NumberFormatter(numberFormat);
        numberFormatter.setValueClass(Integer.class);
        numberFormatter.setAllowsInvalid(false);
        numberFormatter.setMinimum(0);
        numberFormatter.setMaximum(Integer.MAX_VALUE);
        tfMaxColumnWidth = new JFormattedTextField(numberFormatter);

        numberFormatter = new NumberFormatter(numberFormat);
        numberFormatter.setValueClass(Integer.class);
        numberFormatter.setAllowsInvalid(false);
        numberFormatter.setMinimum(MIN_TABLE_COLUMN_SIZE);
        numberFormatter.setMaximum(MAX_TABLE_COLUMN_SIZE);
        tfDefaultColumnWidth = new JFormattedTextField(numberFormatter);
    }
}
