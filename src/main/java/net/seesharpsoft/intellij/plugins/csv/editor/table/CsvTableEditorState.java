package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CsvTableEditorState implements FileEditorState {

    private int[] columnWidths;
    private Boolean showInfoPanel;
    private Integer rowHeight;

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
        return showInfoPanel == null ? CsvEditorSettings.getInstance().showTableEditorInfoPanel() : showInfoPanel;
    }

    public void setShowInfoPanel(boolean showInfoPanelArg) {
        showInfoPanel = showInfoPanelArg;
    }

    public int getRowHeight() {
        if (rowHeight == null) {
            rowHeight = CsvEditorSettings.getInstance().getTableEditorRowHeight();
        }
        return rowHeight;
    }

    public void setRowHeight(int rowHeightArg) {
        rowHeight = rowHeightArg;
    }

    @Override
    public boolean canBeMergedWith(FileEditorState fileEditorState, FileEditorStateLevel fileEditorStateLevel) {
        return false;
    }

    public void write(@NotNull Project project, @NotNull Element element) {
        element.setAttribute("showInfoPanel", "" + showInfoPanel());
        element.setAttribute("rowHeight", "" + getRowHeight());
        for (int i = 0; i < columnWidths.length; ++i) {
            Element cwElement = new Element("column");

            cwElement.setAttribute("index", "" + i);
            cwElement.setAttribute("width", "" + getColumnWidths()[i]);

            element.addContent(cwElement);
        }
    }

    public static CsvTableEditorState create(@NotNull Element element, @NotNull Project project, @NotNull VirtualFile file) {
        CsvTableEditorState state = new CsvTableEditorState();

        Attribute attribute = element.getAttribute("showInfoPanel");
        state.setShowInfoPanel(
                attribute == null ? CsvEditorSettings.getInstance().showTableEditorInfoPanel() : Boolean.parseBoolean(attribute.getValue())
        );

        state.setRowHeight(
                StringUtilRt.parseInt(element.getAttributeValue("rowHeight"), CsvEditorSettings.getInstance().getTableEditorRowHeight())
        );

        List<Element> columnWidthElements = element.getChildren("column");
        int[] columnWidths = new int[columnWidthElements.size()];
        int defaultColumnWidth = CsvEditorSettings.getInstance().getTableDefaultColumnWidth();
        for (int i = 0; i < columnWidthElements.size(); ++i) {
            Element columnElement = columnWidthElements.get(i);
            int index = StringUtilRt.parseInt(columnElement.getAttributeValue("index"), i);
            columnWidths[index] = StringUtilRt.parseInt(columnElement.getAttributeValue("width"), defaultColumnWidth);
        }
        state.setColumnWidths(columnWidths);

        return state;
    }

}
