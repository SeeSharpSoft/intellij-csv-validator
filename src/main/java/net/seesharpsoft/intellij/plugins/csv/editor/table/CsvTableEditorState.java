package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CsvTableEditorState implements FileEditorState {

    private int[] columnWidths;
    private Integer rowHeight;
    private int frozenColumnCount;

    public CsvTableEditorState() {
        columnWidths = new int[0];
    }

    public CsvTableEditorState(CsvTableEditorState state) {
        columnWidths = state.columnWidths.clone();
        rowHeight = state.getRowHeight();
        frozenColumnCount = state.frozenColumnCount;
    }

    public int[] getColumnWidths() {
        return this.columnWidths;
    }

    public void setColumnWidths(int[] widths) {
        this.columnWidths = widths;
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

    public int getFrozenColumnCount() {
        return frozenColumnCount;
    }

    public void setFrozenColumnCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Frozen column count must not be negative");
        }
        frozenColumnCount = count;
    }

    @Override
    public boolean canBeMergedWith(FileEditorState fileEditorState, FileEditorStateLevel fileEditorStateLevel) {
        return false;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof CsvTableEditorState state)) return false;
        return Arrays.equals(columnWidths, state.columnWidths) &&
                getRowHeight() == state.getRowHeight() && frozenColumnCount == state.frozenColumnCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(columnWidths), getRowHeight(), frozenColumnCount);
    }

    public void write(@NotNull Project project, @NotNull Element element) {
        element.setAttribute("rowHeight", "" + getRowHeight());
        element.setAttribute("frozenColumnCount", "" + getFrozenColumnCount());
        for (int i = 0; i < columnWidths.length; ++i) {
            Element cwElement = new Element("column");

            cwElement.setAttribute("index", "" + i);
            cwElement.setAttribute("width", "" + getColumnWidths()[i]);

            element.addContent(cwElement);
        }
    }

    public static CsvTableEditorState create(@NotNull Element element, @NotNull Project project, @NotNull VirtualFile file) {
        CsvTableEditorState state = new CsvTableEditorState();

        state.setRowHeight(
                StringUtilRt.parseInt(element.getAttributeValue("rowHeight"), CsvEditorSettings.getInstance().getTableEditorRowHeight())
        );
        state.setFrozenColumnCount(Math.max(0, StringUtilRt.parseInt(element.getAttributeValue("frozenColumnCount"), 0)));

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
