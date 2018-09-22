package net.seesharpsoft.intellij.plugins.csv.editor;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.ui.CheckBoxWithColorChooser;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class CsvEditorSettingsPanel implements SearchableConfigurable {
    private JCheckBox cbCaretRowShown;
    private JPanel myMainPanel;
    private JCheckBox cbUseSoftWraps;
    private JCheckBox cbColumnHighlighting;
    private CheckBoxWithColorChooser cbTabHighlightColor;

    @NotNull
    @Override
    public String getId() {
        return "Csv.Editor.Settings";
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "CSV/TSV Editor Settings";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return myMainPanel;
    }

    // ensure downward compatibility
    @Override
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
                !Objects.equals(cbTabHighlightColor.getColor(), csvEditorSettingsExternalizable.getTabHighlightColor());
    }

    @Override
    public void reset() {
        CsvEditorSettingsExternalizable csvEditorSettingsExternalizable = CsvEditorSettingsExternalizable.getInstance();
        cbCaretRowShown.setSelected(csvEditorSettingsExternalizable.isCaretRowShown());
        cbUseSoftWraps.setSelected(csvEditorSettingsExternalizable.isUseSoftWraps());
        cbColumnHighlighting.setSelected(csvEditorSettingsExternalizable.isColumnHighlightingEnabled());
        cbTabHighlightColor.setSelected(csvEditorSettingsExternalizable.isHighlightTabSeparator());
        cbTabHighlightColor.setColor(csvEditorSettingsExternalizable.getTabHighlightColor());
    }

    @Override
    public void apply() throws ConfigurationException {
        CsvEditorSettingsExternalizable csvEditorSettingsExternalizable = CsvEditorSettingsExternalizable.getInstance();
        csvEditorSettingsExternalizable.setCaretRowShown(cbCaretRowShown.isSelected());
        csvEditorSettingsExternalizable.setUseSoftWraps(cbUseSoftWraps.isSelected());
        csvEditorSettingsExternalizable.setColumnHighlightingEnabled(cbColumnHighlighting.isSelected());
        csvEditorSettingsExternalizable.setHighlightTabSeparator(cbTabHighlightColor.isSelected());
        csvEditorSettingsExternalizable.setTabHighlightColor(cbTabHighlightColor.getColor());
    }

    private void createUIComponents() {
        cbTabHighlightColor = new CheckBoxWithColorChooser("Highlight tab separator   ");
        cbTabHighlightColor.setColor(Color.CYAN);
    }
}
