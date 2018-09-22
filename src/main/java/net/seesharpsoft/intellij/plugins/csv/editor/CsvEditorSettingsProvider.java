package net.seesharpsoft.intellij.plugins.csv.editor;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class CsvEditorSettingsProvider implements SearchableConfigurable {

    private CsvEditorSettingsPanel myCsvEditorSettingsPanel;

    public String getDisplayName() {
        return "CSV/TSV Editor";
    }

    public String getHelpTopic() {
        return "Editor Options for CSV/TSV files";
    }

    @NotNull
    public String getId() {
        return "CSV_EDITOR_OPTIONS";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        this.myCsvEditorSettingsPanel = new CsvEditorSettingsPanel();
        return this.myCsvEditorSettingsPanel.createComponent();
    }

    @Override
    public boolean isModified() {
        return this.myCsvEditorSettingsPanel != null && this.myCsvEditorSettingsPanel.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {
        if (this.myCsvEditorSettingsPanel != null) {
            this.myCsvEditorSettingsPanel.apply();
        }
    }

    @Override
    public void reset() {
        if (this.myCsvEditorSettingsPanel != null) {
            this.myCsvEditorSettingsPanel.reset();
        }
    }

    @Override
    public void disposeUIResources() {
        this.myCsvEditorSettingsPanel.disposeUIResources();
        this.myCsvEditorSettingsPanel = null;
    }
}
