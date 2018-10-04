package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import net.seesharpsoft.intellij.plugins.csv.editor.CsvEditorSettingsExternalizable;

public class CsvTableEditorState implements FileEditorState {

    private int[] columnWidths;
    private Boolean showInfoPanel;
    private int rowLines;

    public CsvTableEditorState() {
        this.columnWidths = new int[0];
        rowLines = 0;
    }

    public int[] getColumnWidths() {
        return this.columnWidths;
    }
    public void setColumnWidths(int[] widths) {
        this.columnWidths = widths;
    }

    public boolean showInfoPanel() {
        if (showInfoPanel == null) {
            return CsvEditorSettingsExternalizable.getInstance().showTableEditorInfoPanel();
        }
        return showInfoPanel;
    }
    public void setShowInfoPanel(boolean showInfoPanelArg) {
        showInfoPanel = showInfoPanelArg;
    }

    public int getRowLines() {
        return this.rowLines;
    }
    public void setRowLines(int rowLinesArg) {
        this.rowLines = rowLinesArg;
    }

    @Override
    public boolean canBeMergedWith(FileEditorState fileEditorState, FileEditorStateLevel fileEditorStateLevel) {
        return false;
    }
}
