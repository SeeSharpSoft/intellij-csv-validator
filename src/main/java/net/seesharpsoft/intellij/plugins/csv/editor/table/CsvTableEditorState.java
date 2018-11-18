package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import net.seesharpsoft.intellij.plugins.csv.editor.CsvEditorSettingsExternalizable;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditor.INITIAL_COLUMN_WIDTH;

public class CsvTableEditorState implements FileEditorState {

    private int[] columnWidths;
    private Boolean showInfoPanel;
    private Integer rowLines;

    public CsvTableEditorState() {
        columnWidths = new int[0];
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
        if (rowLines == null) {
            rowLines = CsvEditorSettingsExternalizable.getInstance().getTableEditorRowHeight();
        }
        return rowLines;
    }

    public void setRowLines(int rowLinesArg) {
        rowLines = rowLinesArg;
    }

    @Override
    public boolean canBeMergedWith(FileEditorState fileEditorState, FileEditorStateLevel fileEditorStateLevel) {
        return false;
    }

    public void write(@NotNull Project project, @NotNull Element element) {
        element.setAttribute("showInfoPanel", "" + showInfoPanel());
        element.setAttribute("rowLines", "" + getRowLines());
        for (int i = 0; i < columnWidths.length; ++i) {
            Element cwElement = new Element("column");

            cwElement.setAttribute("index", "" + i);
            cwElement.setAttribute("width", "" + getColumnWidths()[i]);

            element.addContent(cwElement);
        }
    }

    public static CsvTableEditorState create(@NotNull Element element, @NotNull Project project, @NotNull VirtualFile file) {
        CsvTableEditorState state = new CsvTableEditorState();

        state.setShowInfoPanel(
                StringUtilRt.parseBoolean(element.getAttributeValue("showInfoPanel"), CsvEditorSettingsExternalizable.getInstance().showTableEditorInfoPanel())
        );
        state.setRowLines(
                StringUtilRt.parseInt(element.getAttributeValue("rowLines"), CsvEditorSettingsExternalizable.getInstance().getTableEditorRowHeight())
        );

        List<Element> columnWidthElements = element.getChildren("column");
        int[] columnWidths = new int[columnWidthElements.size()];
        for (int i = 0; i < columnWidthElements.size(); ++i) {
            Element columnElement = columnWidthElements.get(i);
            int index = StringUtilRt.parseInt(columnElement.getAttributeValue("index"), i);
            columnWidths[index] = StringUtilRt.parseInt(columnElement.getAttributeValue("width"), INITIAL_COLUMN_WIDTH);
        }
        state.setColumnWidths(columnWidths);

        return state;
    }

}
