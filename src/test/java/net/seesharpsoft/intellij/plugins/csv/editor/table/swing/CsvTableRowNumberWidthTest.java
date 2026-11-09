package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.util.ui.UIUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvFileType;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditorState;
import org.jdom.Element;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import static org.junit.Assert.assertArrayEquals;

public class CsvTableRowNumberWidthTest extends CsvTableEditorSwingTestBase {

    public void testRowNumberHeaderKeepsItsTableAndStyleAfterUiRefresh() {
        JTable rowNumbers = layoutEditor(800, 400);
        JTableHeader header = rowNumbers.getTableHeader();
        header.updateUI();
        assertSame(rowNumbers, header.getTable());
        JTableHeader dataHeader = fileEditor.getTable().getTableHeader();
        assertEquals(dataHeader.getBackground(), header.getBackground());
        assertEquals(dataHeader.getForeground(), header.getForeground());
        assertEquals(dataHeader.getFont(), header.getFont());
        int originalWidth = rowNumbers.getColumnModel().getColumn(0).getWidth();
        dragHeaderEdge(header, 20);
        assertEquals(originalWidth + 20, rowNumbers.getColumnModel().getColumn(0).getWidth());
    }

    public void testDraggingRowNumberHeaderResizesGutterWithoutChangingCsvColumns() {
        JTable rowNumbers = layoutEditor(800, 400);
        JScrollPane scrollPane = fileEditor.getTableScrollPane();
        int originalWidth = scrollPane.getRowHeader().getWidth();
        int[] columnWidths = fileEditor.getTableEditorState().getColumnWidths().clone();
        String originalText = myFixture.getFile().getText();
        assertTrue(originalWidth > 0);
        assertEquals(originalWidth, rowNumbers.getColumnModel().getColumn(0).getWidth());

        dragHeaderEdge(rowNumbers.getTableHeader(), 40);
        layoutRecursively(scrollPane);
        assertEquals("Dragging the row-number header must widen the visible gutter",
                originalWidth + 40, scrollPane.getRowHeader().getWidth());
        assertEquals(originalWidth + 40, rowNumbers.getColumnModel().getColumn(0).getWidth());

        dragHeaderEdge(rowNumbers.getTableHeader(), -20);
        layoutRecursively(scrollPane);
        assertEquals("Dragging back must narrow the visible gutter",
                originalWidth + 20, scrollPane.getRowHeader().getWidth());
        assertEquals(originalWidth + 20, rowNumbers.getColumnModel().getColumn(0).getWidth());
        assertArrayEquals(columnWidths, fileEditor.getTableEditorState().getColumnWidths());
        for (int column = 0; column < columnWidths.length; column++) {
            assertEquals(columnWidths[column], fileEditor.getTable().getColumnModel().getColumn(column).getWidth());
        }
        assertEquals(originalText, myFixture.getFile().getText());
    }

    public void testRowNumberWidthIsSavedAndRestoredForTheFile() {
        JTable rowNumbers = layoutEditor(800, 400);
        int width = fileEditor.getTableScrollPane().getRowHeader().getWidth() + 40;
        dragHeaderEdge(rowNumbers.getTableHeader(), 40);
        CsvTableEditorState state = (CsvTableEditorState) fileEditor.getState(FileEditorStateLevel.FULL);
        assertEquals(width, state.getRowNumberColumnWidth());
        Element element = new Element("state");
        state.write(getProject(), element);
        CsvTableEditorState restored = CsvTableEditorState.create(element, getProject(), fileEditor.getFile());
        assertEquals(state, restored);
        assertEquals(state.hashCode(), restored.hashCode());

        fileEditor.dispose();
        fileEditor = createEditor();
        fileEditor.setState(restored);
        fileEditor.selectNotify();
        layoutEditor(800, 400);
        assertGutterWidth(width);
    }

    public void testScrollingResizingAndRefreshingKeepManuallyChosenWidth() {
        configureLargeTable();
        JTable rowNumbers = layoutEditor(800, 180);
        int width = fileEditor.getTableScrollPane().getRowHeader().getWidth() + 40;
        dragHeaderEdge(rowNumbers.getTableHeader(), 40);
        layoutEditor(600, 150);
        assertGutterWidth(width);

        fileEditor.getTableScrollPane().getViewport().setViewPosition(
                new Point(0, fileEditor.getTable().getCellRect(900, 0, true).y));
        layoutEditor(600, 150);
        assertGutterWidth(width);

        fileEditor.getTable().getModel().updateTableImmediately();
        layoutEditor(600, 150);
        assertGutterWidth(width);
        assertEquals(width, fileEditor.getTableEditorState().getRowNumberColumnWidth());
    }

    public void testOldEditorStateKeepsAutomaticRowNumberSizing() {
        configureLargeTable();
        CsvTableEditorState oldState = CsvTableEditorState.create(
                new Element("state"), getProject(), fileEditor.getFile());
        fileEditor.setState(oldState);
        layoutEditor(800, 100);
        int originalWidth = fileEditor.getTableScrollPane().getRowHeader().getWidth();

        fileEditor.getTableScrollPane().getViewport().setViewPosition(
                new Point(0, fileEditor.getTable().getCellRect(900, 0, true).y));
        layoutEditor(800, 100);
        assertTrue("Automatic sizing must fit the larger visible row numbers",
                fileEditor.getTableScrollPane().getRowHeader().getWidth() > originalWidth);

        fileEditor.getTableScrollPane().getViewport().setViewPosition(new Point(0, 0));
        layoutEditor(800, 100);
        assertGutterWidth(originalWidth);
        assertEquals(0, fileEditor.getTableEditorState().getRowNumberColumnWidth());
    }

    private void configureLargeTable() {
        fileEditor.dispose();
        myFixture.configureByText(CsvFileType.INSTANCE, "a,b\n" + "1,2\n".repeat(1000));
        fileEditor = createEditor();
        fileEditor.selectNotify();
    }

    private JTable layoutEditor(int width, int height) {
        JScrollPane scrollPane = fileEditor.getTableScrollPane();
        scrollPane.setSize(width, height);
        layoutRecursively(scrollPane);
        UIUtil.dispatchAllInvocationEvents();
        layoutRecursively(scrollPane);
        return (JTable) scrollPane.getRowHeader().getView();
    }

    private void assertGutterWidth(int width) {
        JViewport rowHeader = fileEditor.getTableScrollPane().getRowHeader();
        assertEquals(width, rowHeader.getWidth());
        JTable rowNumbers = (JTable) rowHeader.getView();
        assertEquals(width, rowNumbers.getColumnModel().getColumn(0).getWidth());
        assertEquals(width, rowNumbers.getTableHeader().getWidth());
    }

    private static void dragHeaderEdge(JTableHeader header, int distance) {
        Rectangle cell = header.getHeaderRect(0);
        int edge = cell.x + cell.width - 1;
        header.dispatchEvent(new MouseEvent(header, MouseEvent.MOUSE_MOVED, 0, 0,
                edge, 5, 0, false, MouseEvent.NOBUTTON));
        header.dispatchEvent(new MouseEvent(header, MouseEvent.MOUSE_PRESSED, 0, InputEvent.BUTTON1_DOWN_MASK,
                edge, 5, 1, false, MouseEvent.BUTTON1));
        header.dispatchEvent(new MouseEvent(header, MouseEvent.MOUSE_DRAGGED, 0, InputEvent.BUTTON1_DOWN_MASK,
                edge + distance, 5, 0, false, MouseEvent.NOBUTTON));
        header.dispatchEvent(new MouseEvent(header, MouseEvent.MOUSE_RELEASED, 0, 0,
                edge + distance, 5, 1, false, MouseEvent.BUTTON1));
    }

    private static void layoutRecursively(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) layoutRecursively(nested);
        }
    }
}
