package net.seesharpsoft.intellij.plugins.csv.settings;

import com.intellij.application.options.editor.EditorOptionsProvider;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.CheckBoxWithColorChooser;
import com.intellij.ui.JBColor;
import com.intellij.util.FileContentUtilCore;
import net.seesharpsoft.intellij.plugins.csv.components.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.components.CsvValueSeparator;
import net.seesharpsoft.intellij.ui.CustomDisplayListCellRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import static net.seesharpsoft.intellij.plugins.csv.CsvPluginManager.getLocalizedText;

public class CsvEditorSettingsProvider implements EditorOptionsProvider, SearchableConfigurable {

    public static final String CSV_EDITOR_SETTINGS_ID = "Csv.Editor.Settings";

    public static final int MIN_TABLE_COLUMN_SIZE = 10;
    public static final int MAX_TABLE_COLUMN_SIZE = 10000;

    private JCheckBox cbCaretRowShown;
    private JCheckBox cbHighlightActiveColumn;
    private JLabel lbMaxHighlightingRows;
    private JFormattedTextField tfMaxHighlightingRows;
    private JPanel myMainPanel;
    private JCheckBox cbUseSoftWraps;
    private CheckBoxWithColorChooser cbTabHighlightColor;
    private JCheckBox cbShowInfoBalloonCheckBox;
    private JComboBox<CsvEditorSettings.EditorPrio> cbEditorUsage;
    private JCheckBox cbSyncSelectedField;
    private JCheckBox cbQuotingEnforced;
    private JCheckBox cbZeroBasedColumnNumbering;
    private JFormattedTextField tfMaxColumnWidth;
    private JFormattedTextField tfDefaultColumnWidth;
    private JComboBox<CsvEscapeCharacter> comboEscapeCharacter;
    private JComboBox<CsvValueSeparator> comboValueSeparator;
    private JCheckBox cbKeepTrailingWhitespaces;
    private JTextField tfCommentIndicator;
    private JComboBox<CsvEditorSettings.ValueColoring> comboValueColoring;
    private JCheckBox cbAutoDetectSeparator;
    private JFormattedTextField tfDefaultRowHeight;

    public CsvEditorSettingsProvider() {
        cbHighlightActiveColumn.addItemListener(event -> updateColumnHighlightingControls());
        updateColumnHighlightingControls();
    }

    private void updateColumnHighlightingControls() {
        boolean enabled = cbHighlightActiveColumn.isSelected();
        lbMaxHighlightingRows.setEnabled(enabled);
        tfMaxHighlightingRows.setEnabled(enabled);
    }

    @NotNull
    @Override
    public String getId() {
        return CSV_EDITOR_SETTINGS_ID;
    }

    @Override
    public String getDisplayName() {
        return getLocalizedText("settings.title");
    }

    @Override
    public String getHelpTopic() {
        return getLocalizedText("settings.editor.help");
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return myMainPanel;
    }

    @Override
    public boolean isModified() {
        CsvEditorSettings csvEditorSettings = CsvEditorSettings.getInstance();
        return Configurable.isCheckboxModified(cbCaretRowShown, csvEditorSettings.isCaretRowShown()) ||
                Configurable.isCheckboxModified(cbHighlightActiveColumn, csvEditorSettings.isHighlightActiveColumn()) ||
                isMaxHighlightingRowsModified(csvEditorSettings.getMaxHighlightingRows()) ||
                Configurable.isCheckboxModified(cbUseSoftWraps, csvEditorSettings.isUseSoftWraps()) ||
                Configurable.isCheckboxModified(cbShowInfoBalloonCheckBox, csvEditorSettings.isShowInfoBalloon()) ||
                cbTabHighlightColor.isSelected() != csvEditorSettings.isHighlightTabSeparator() ||
                !Objects.equals(cbTabHighlightColor.getColor(), csvEditorSettings.getTabHighlightColor()) ||
                !tfDefaultRowHeight.getValue().equals(csvEditorSettings.getTableEditorRowHeight()) ||
                Configurable.isCheckboxModified(cbQuotingEnforced, csvEditorSettings.isQuotingEnforced()) ||
                !Objects.equals(cbEditorUsage.getSelectedItem(), csvEditorSettings.getEditorPrio()) ||
                Configurable.isCheckboxModified(cbSyncSelectedField, csvEditorSettings.isSyncSelectedField()) ||
                Configurable.isCheckboxModified(cbZeroBasedColumnNumbering, csvEditorSettings.isZeroBasedColumnNumbering()) ||
                !tfMaxColumnWidth.getValue().equals(csvEditorSettings.getTableAutoMaxColumnWidth()) ||
                !tfDefaultColumnWidth.getValue().equals(csvEditorSettings.getTableDefaultColumnWidth()) ||
                !Objects.equals(comboEscapeCharacter.getSelectedItem(), csvEditorSettings.getDefaultEscapeCharacter()) ||
                !Objects.equals(comboValueSeparator.getSelectedItem(), csvEditorSettings.getDefaultValueSeparator()) ||
                Configurable.isCheckboxModified(cbKeepTrailingWhitespaces, csvEditorSettings.getKeepTrailingSpaces()) ||
                Configurable.isFieldModified(tfCommentIndicator, csvEditorSettings.getCommentIndicator()) ||
                !Objects.equals(comboValueColoring.getSelectedItem(), csvEditorSettings.getValueColoring()) ||
                Configurable.isCheckboxModified(cbAutoDetectSeparator, csvEditorSettings.isAutoDetectValueSeparator());
    }

    private boolean isMaxHighlightingRowsModified(int maxHighlightingRows) {
        try {
            return !Objects.equals(tfMaxHighlightingRows.getFormatter().stringToValue(tfMaxHighlightingRows.getText()),
                    maxHighlightingRows);
        } catch (ParseException exception) {
            return true;
        }
    }

    @Override
    public void reset() {
        CsvEditorSettings csvEditorSettings = CsvEditorSettings.getInstance();
        cbCaretRowShown.setSelected(csvEditorSettings.isCaretRowShown());
        cbHighlightActiveColumn.setSelected(csvEditorSettings.isHighlightActiveColumn());
        tfMaxHighlightingRows.setValue(csvEditorSettings.getMaxHighlightingRows());
        cbUseSoftWraps.setSelected(csvEditorSettings.isUseSoftWraps());
        cbShowInfoBalloonCheckBox.setSelected(csvEditorSettings.isShowInfoBalloon());
        cbTabHighlightColor.setSelected(csvEditorSettings.isHighlightTabSeparator());
        cbTabHighlightColor.setColor(csvEditorSettings.getTabHighlightColor());
        tfDefaultRowHeight.setValue(csvEditorSettings.getTableEditorRowHeight());
        cbEditorUsage.setSelectedItem(csvEditorSettings.getEditorPrio());
        cbSyncSelectedField.setSelected(csvEditorSettings.isSyncSelectedField());
        cbQuotingEnforced.setSelected(csvEditorSettings.isQuotingEnforced());
        cbZeroBasedColumnNumbering.setSelected(csvEditorSettings.isZeroBasedColumnNumbering());
        tfMaxColumnWidth.setValue(csvEditorSettings.getTableAutoMaxColumnWidth());
        tfDefaultColumnWidth.setValue(csvEditorSettings.getTableDefaultColumnWidth());
        comboEscapeCharacter.setSelectedItem(csvEditorSettings.getDefaultEscapeCharacter());
        comboValueSeparator.setSelectedItem(csvEditorSettings.getDefaultValueSeparator());
        cbKeepTrailingWhitespaces.setSelected(csvEditorSettings.getKeepTrailingSpaces());
        tfCommentIndicator.setText(csvEditorSettings.getCommentIndicator());
        comboValueColoring.setSelectedItem(csvEditorSettings.getValueColoring());
        cbAutoDetectSeparator.setSelected(csvEditorSettings.isAutoDetectValueSeparator());
    }

    @Override
    public void apply() throws ConfigurationException {
        try {
            tfMaxHighlightingRows.commitEdit();
        } catch (ParseException exception) {
            throw new ConfigurationException(getLocalizedText("highlight.active.column.maximum.rows.invalid"));
        }
        CsvEditorSettings csvEditorSettings = CsvEditorSettings.getInstance();
        csvEditorSettings.setCaretRowShown(cbCaretRowShown.isSelected());
        csvEditorSettings.setHighlightActiveColumn(cbHighlightActiveColumn.isSelected());
        csvEditorSettings.setMaxHighlightingRows((int) tfMaxHighlightingRows.getValue());
        csvEditorSettings.setUseSoftWraps(cbUseSoftWraps.isSelected());
        csvEditorSettings.setShowInfoBalloon(cbShowInfoBalloonCheckBox.isSelected());
        csvEditorSettings.setHighlightTabSeparator(cbTabHighlightColor.isSelected());
        csvEditorSettings.setTabHighlightColor(cbTabHighlightColor.getColor());
        csvEditorSettings.setTableEditorRowHeight((int) tfDefaultRowHeight.getValue());
        csvEditorSettings.setEditorPrio((CsvEditorSettings.EditorPrio) cbEditorUsage.getSelectedItem());
        csvEditorSettings.setSyncSelectedField(cbSyncSelectedField.isSelected());
        csvEditorSettings.setQuotingEnforced(cbQuotingEnforced.isSelected());
        csvEditorSettings.setZeroBasedColumnNumbering(cbZeroBasedColumnNumbering.isSelected());
        csvEditorSettings.setTableAutoMaxColumnWidth((int) tfMaxColumnWidth.getValue());
        csvEditorSettings.setTableDefaultColumnWidth((int) tfDefaultColumnWidth.getValue());
        csvEditorSettings.setDefaultEscapeCharacter((CsvEscapeCharacter) comboEscapeCharacter.getSelectedItem());
        csvEditorSettings.setDefaultValueSeparator(
                comboValueSeparator.getSelectedItem() instanceof CsvValueSeparator ?
                        (CsvValueSeparator) comboValueSeparator.getSelectedItem() :
                        CsvValueSeparator.create((String) comboValueSeparator.getSelectedItem())
        );
        csvEditorSettings.setKeepTrailingSpaces(cbKeepTrailingWhitespaces.isSelected());
        csvEditorSettings.setCommentIndicator(tfCommentIndicator.getText());
        csvEditorSettings.setValueColoring((CsvEditorSettings.ValueColoring) comboValueColoring.getSelectedItem());
        csvEditorSettings.setAutoDetectValueSeparator(cbAutoDetectSeparator.isSelected());

        this.refreshOpenEditors();
    }

    protected void refreshOpenEditors() {
        FileContentUtilCore.reparseFiles(
                Arrays.stream(ProjectManager.getInstance().getOpenProjects())
                        .map(FileEditorManager::getInstance)
                        .flatMap(manager -> Arrays.stream(manager.getOpenFiles()))
                        .collect(Collectors.toList())
        );
    }

    protected void createUIComponents() {
        cbEditorUsage = new ComboBox<>(CsvEditorSettings.EditorPrio.values());
        cbEditorUsage.setRenderer(new CustomDisplayListCellRenderer<>(CsvEditorSettings.EditorPrio::getDisplay));

        comboEscapeCharacter = new ComboBox<>(CsvEscapeCharacter.values());
        comboEscapeCharacter.setRenderer(new CustomDisplayListCellRenderer<>(CsvEscapeCharacter::getDisplay));

        comboValueSeparator = new ComboBox<>(CsvValueSeparator.values());
        comboValueSeparator.setRenderer(new CustomDisplayListCellRenderer<>(CsvValueSeparator::getDisplay));

        comboValueColoring = new ComboBox<>(CsvEditorSettings.ValueColoring.values());
        comboValueColoring.setRenderer(new CustomDisplayListCellRenderer<>(CsvEditorSettings.ValueColoring::getDisplay));

        cbTabHighlightColor = new CheckBoxWithColorChooser(getLocalizedText("settings.editor.highlight.tab.separator"));
        cbTabHighlightColor.setColor(JBColor.CYAN);

        NumberFormatter highlightingRowsFormatter = new NumberFormatter(NumberFormat.getIntegerInstance()) {
            @Override
            public Object stringToValue(String text) throws ParseException {
                ParsePosition position = new ParsePosition(0);
                Object value = getFormat().parseObject(text, position);
                if (!(value instanceof Number number) || position.getIndex() != text.length()
                        || number.doubleValue() != number.longValue()
                        || number.longValue() < -1 || number.longValue() > Integer.MAX_VALUE) {
                    throw new ParseException(getLocalizedText("highlight.active.column.maximum.rows.invalid"),
                            position.getIndex());
                }
                return number.intValue();
            }
        };
        highlightingRowsFormatter.setValueClass(Integer.class);
        highlightingRowsFormatter.setAllowsInvalid(true);
        highlightingRowsFormatter.setCommitsOnValidEdit(true);
        highlightingRowsFormatter.setMinimum(-1);
        highlightingRowsFormatter.setMaximum(Integer.MAX_VALUE);
        tfMaxHighlightingRows = new JFormattedTextField(highlightingRowsFormatter);
        tfMaxHighlightingRows.setFocusLostBehavior(JFormattedTextField.PERSIST);

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
