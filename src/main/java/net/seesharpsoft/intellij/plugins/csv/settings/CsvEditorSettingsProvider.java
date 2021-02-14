package net.seesharpsoft.intellij.plugins.csv.settings;

import com.intellij.application.options.editor.EditorOptionsProvider;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.CheckBoxWithColorChooser;
import net.seesharpsoft.intellij.plugins.csv.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.CsvValueSeparator;
import net.seesharpsoft.intellij.ui.CustomDisplayListCellRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Objects;

public class CsvEditorSettingsProvider implements EditorOptionsProvider {

    public static final String CSV_EDITOR_SETTINGS_ID = "Csv.Editor.Settings";

    public static final int MIN_TABLE_COLUMN_SIZE = 10;
    public static final int MAX_TABLE_COLUMN_SIZE = 10000;

    private JCheckBox cbCaretRowShown;
    private JPanel myMainPanel;
    private JCheckBox cbUseSoftWraps;
    private CheckBoxWithColorChooser cbTabHighlightColor;
    private JCheckBox cbShowInfoBalloonCheckBox;
    private JCheckBox cbShowInfoPanel;
    private JComboBox cbRowHeight;
    private JComboBox cbEditorUsage;
    private JCheckBox cbQuotingEnforced;
    private JCheckBox cbZeroBasedColumnNumbering;
    private JCheckBox cbFileEndLineBreak;
    private JFormattedTextField tfMaxColumnWidth;
    private JFormattedTextField tfDefaultColumnWidth;
    private JCheckBox cbAdjustColumnWidthOnOpen;
    private JComboBox comboEscapeCharacter;
    private JComboBox comboValueSeparator;
    private JCheckBox cbKeepTrailingWhitespaces;
    private JTextField tfCommentIndicator;
    private JComboBox comboValueColoring;
    private JCheckBox cbHeaderRowFixed;
    private JCheckBox cbAutoDetectSeparator;

    @NotNull
    @Override
    public String getId() {
        return CSV_EDITOR_SETTINGS_ID;
    }

    @Override
    public String getDisplayName() {
        return "CSV/TSV/PSV";
    }

    @Override
    public String getHelpTopic() {
        return "Editor Options for CSV/TSV/PSV files";
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
        CsvEditorSettings csvEditorSettings = CsvEditorSettings.getInstance();
        return isModified(cbCaretRowShown, csvEditorSettings.isCaretRowShown()) ||
                isModified(cbUseSoftWraps, csvEditorSettings.isUseSoftWraps()) ||
                isModified(cbShowInfoBalloonCheckBox, csvEditorSettings.isShowInfoBalloon()) ||
                isModified(cbShowInfoPanel, csvEditorSettings.showTableEditorInfoPanel()) ||
                cbTabHighlightColor.isSelected() != csvEditorSettings.isHighlightTabSeparator() ||
                !Objects.equals(cbTabHighlightColor.getColor(), csvEditorSettings.getTabHighlightColor()) ||
                !Objects.equals(cbRowHeight.getSelectedIndex(), csvEditorSettings.getTableEditorRowHeight()) ||
                !Objects.equals(cbEditorUsage.getSelectedIndex(), csvEditorSettings.getEditorPrio().ordinal()) ||
                isModified(cbQuotingEnforced, csvEditorSettings.isQuotingEnforced()) ||
                !Objects.equals(cbEditorUsage.getSelectedIndex(), csvEditorSettings.getEditorPrio().ordinal()) ||
                isModified(cbZeroBasedColumnNumbering, csvEditorSettings.isZeroBasedColumnNumbering()) ||
                isModified(cbFileEndLineBreak, csvEditorSettings.isFileEndLineBreak()) ||
                !tfMaxColumnWidth.getValue().equals(csvEditorSettings.getTableAutoMaxColumnWidth()) ||
                !tfDefaultColumnWidth.getValue().equals(csvEditorSettings.getTableDefaultColumnWidth()) ||
                isModified(cbAdjustColumnWidthOnOpen, csvEditorSettings.isTableAutoColumnWidthOnOpen()) ||
                !Objects.equals(comboEscapeCharacter.getSelectedItem(), csvEditorSettings.getDefaultEscapeCharacter()) ||
                !Objects.equals(comboValueSeparator.getSelectedItem(), csvEditorSettings.getDefaultValueSeparator()) ||
                isModified(cbKeepTrailingWhitespaces, csvEditorSettings.getKeepTrailingSpaces()) ||
                isModified(tfCommentIndicator, csvEditorSettings.getCommentIndicator()) ||
                !Objects.equals(comboValueColoring.getSelectedItem(), csvEditorSettings.getValueColoring()) ||
                isModified(cbHeaderRowFixed, csvEditorSettings.isHeaderRowFixed()) ||
                isModified(cbAutoDetectSeparator, csvEditorSettings.isAutoDetectValueSeparator());
    }

    @Override
    public void reset() {
        CsvEditorSettings csvEditorSettings = CsvEditorSettings.getInstance();
        cbCaretRowShown.setSelected(csvEditorSettings.isCaretRowShown());
        cbUseSoftWraps.setSelected(csvEditorSettings.isUseSoftWraps());
        cbShowInfoBalloonCheckBox.setSelected(csvEditorSettings.isShowInfoBalloon());
        cbShowInfoPanel.setSelected(csvEditorSettings.showTableEditorInfoPanel());
        cbTabHighlightColor.setSelected(csvEditorSettings.isHighlightTabSeparator());
        cbTabHighlightColor.setColor(csvEditorSettings.getTabHighlightColor());
        cbRowHeight.setSelectedIndex(csvEditorSettings.getTableEditorRowHeight());
        cbEditorUsage.setSelectedIndex(csvEditorSettings.getEditorPrio().ordinal());
        cbQuotingEnforced.setSelected(csvEditorSettings.isQuotingEnforced());
        cbZeroBasedColumnNumbering.setSelected(csvEditorSettings.isZeroBasedColumnNumbering());
        cbFileEndLineBreak.setSelected(csvEditorSettings.isFileEndLineBreak());
        tfMaxColumnWidth.setValue(csvEditorSettings.getTableAutoMaxColumnWidth());
        tfDefaultColumnWidth.setValue(csvEditorSettings.getTableDefaultColumnWidth());
        cbAdjustColumnWidthOnOpen.setSelected(csvEditorSettings.isTableAutoColumnWidthOnOpen());
        comboEscapeCharacter.setSelectedItem(csvEditorSettings.getDefaultEscapeCharacter());
        comboValueSeparator.setSelectedItem(csvEditorSettings.getDefaultValueSeparator());
        cbKeepTrailingWhitespaces.setSelected(csvEditorSettings.getKeepTrailingSpaces());
        tfCommentIndicator.setText(csvEditorSettings.getCommentIndicator());
        comboValueColoring.setSelectedItem(csvEditorSettings.getValueColoring());
        cbHeaderRowFixed.setSelected(csvEditorSettings.isHeaderRowFixed());
        cbAutoDetectSeparator.setSelected(csvEditorSettings.isAutoDetectValueSeparator());
    }

    @Override
    public void apply() throws ConfigurationException {
        CsvEditorSettings csvEditorSettings = CsvEditorSettings.getInstance();
        csvEditorSettings.setCaretRowShown(cbCaretRowShown.isSelected());
        csvEditorSettings.setUseSoftWraps(cbUseSoftWraps.isSelected());
        csvEditorSettings.setShowInfoBalloon(cbShowInfoBalloonCheckBox.isSelected());
        csvEditorSettings.showTableEditorInfoPanel(cbShowInfoPanel.isSelected());
        csvEditorSettings.setHighlightTabSeparator(cbTabHighlightColor.isSelected());
        csvEditorSettings.setTabHighlightColor(cbTabHighlightColor.getColor());
        csvEditorSettings.setTableEditorRowHeight(cbRowHeight.getSelectedIndex());
        csvEditorSettings.setEditorPrio(CsvEditorSettings.EditorPrio.values()[cbEditorUsage.getSelectedIndex()]);
        csvEditorSettings.setQuotingEnforced(cbQuotingEnforced.isSelected());
        csvEditorSettings.setZeroBasedColumnNumbering(cbZeroBasedColumnNumbering.isSelected());
        csvEditorSettings.setFileEndLineBreak(cbFileEndLineBreak.isSelected());
        csvEditorSettings.setTableAutoMaxColumnWidth((int) tfMaxColumnWidth.getValue());
        csvEditorSettings.setTableDefaultColumnWidth((int) tfDefaultColumnWidth.getValue());
        csvEditorSettings.setTableAutoColumnWidthOnOpen(cbAdjustColumnWidthOnOpen.isSelected());
        csvEditorSettings.setDefaultEscapeCharacter((CsvEscapeCharacter)comboEscapeCharacter.getSelectedItem());
        csvEditorSettings.setDefaultValueSeparator((CsvValueSeparator)comboValueSeparator.getSelectedItem());
        csvEditorSettings.setKeepTrailingSpaces(cbKeepTrailingWhitespaces.isSelected());
        csvEditorSettings.setCommentIndicator(tfCommentIndicator.getText());
        csvEditorSettings.setValueColoring((CsvEditorSettings.ValueColoring) comboValueColoring.getSelectedItem());
        csvEditorSettings.setHeaderRowFixed(cbHeaderRowFixed.isSelected());
        csvEditorSettings.setAutoDetectValueSeparator(cbAutoDetectSeparator.isSelected());
    }

    protected void createUIComponents() {
        comboEscapeCharacter = new ComboBox(CsvEscapeCharacter.values());
        comboEscapeCharacter.setRenderer(new CustomDisplayListCellRenderer<CsvEscapeCharacter>(ec -> ec.getDisplay()));

        comboValueSeparator = new ComboBox(CsvValueSeparator.values());
        comboValueSeparator.setRenderer(new CustomDisplayListCellRenderer<CsvValueSeparator>(ec -> ec.getDisplay()));

        comboValueColoring = new ComboBox(CsvEditorSettings.ValueColoring.values());
        comboValueColoring.setRenderer(new CustomDisplayListCellRenderer<CsvEditorSettings.ValueColoring>(ec -> ec.getDisplay()));

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
