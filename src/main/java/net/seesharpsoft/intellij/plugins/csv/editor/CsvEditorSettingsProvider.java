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
    private JCheckBox cbCaretRowShown;
    private JPanel myMainPanel;
    private JCheckBox cbUseSoftWraps;
    private JCheckBox cbColumnHighlighting;
    private CheckBoxWithColorChooser cbTabHighlightColor;
    private JCheckBox cbShowInfoBalloonCheckBox;

    @NotNull
    @Override
    public String getId() {
        return "Csv.Editor.Settings";
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
                cbTabHighlightColor.isSelected() != csvEditorSettingsExternalizable.isHighlightTabSeparator() ||
                cbShowInfoBalloonCheckBox.isSelected() != csvEditorSettingsExternalizable.isShowInfoBalloon() ||
                !Objects.equals(cbTabHighlightColor.getColor(), csvEditorSettingsExternalizable.getTabHighlightColor());
    }

    @Override
    public void reset() {
        CsvEditorSettingsExternalizable csvEditorSettingsExternalizable = CsvEditorSettingsExternalizable.getInstance();
        cbCaretRowShown.setSelected(csvEditorSettingsExternalizable.isCaretRowShown());
        cbUseSoftWraps.setSelected(csvEditorSettingsExternalizable.isUseSoftWraps());
        cbColumnHighlighting.setSelected(csvEditorSettingsExternalizable.isColumnHighlightingEnabled());
        cbTabHighlightColor.setSelected(csvEditorSettingsExternalizable.isHighlightTabSeparator());
        cbShowInfoBalloonCheckBox.setSelected(csvEditorSettingsExternalizable.isShowInfoBalloon());
        cbTabHighlightColor.setColor(csvEditorSettingsExternalizable.getTabHighlightColor());
    }

    @Override
    public void apply() throws ConfigurationException {
        CsvEditorSettingsExternalizable csvEditorSettingsExternalizable = CsvEditorSettingsExternalizable.getInstance();
        csvEditorSettingsExternalizable.setCaretRowShown(cbCaretRowShown.isSelected());
        csvEditorSettingsExternalizable.setUseSoftWraps(cbUseSoftWraps.isSelected());
        csvEditorSettingsExternalizable.setColumnHighlightingEnabled(cbColumnHighlighting.isSelected());
        csvEditorSettingsExternalizable.setHighlightTabSeparator(cbTabHighlightColor.isSelected());
        csvEditorSettingsExternalizable.setShowInfoBalloon(cbShowInfoBalloonCheckBox.isSelected());
        csvEditorSettingsExternalizable.setTabHighlightColor(cbTabHighlightColor.getColor());
    }

    protected void createUIComponents() {
        cbTabHighlightColor = new CheckBoxWithColorChooser("Highlight tab separator   ");
        cbTabHighlightColor.setColor(Color.CYAN);
    }
}
