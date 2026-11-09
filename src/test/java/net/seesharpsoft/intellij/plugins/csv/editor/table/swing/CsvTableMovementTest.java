package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.JBUI;
import net.seesharpsoft.intellij.plugins.csv.CsvFileType;
import net.seesharpsoft.intellij.plugins.csv.actions.CsvTableEditorActions;
import net.seesharpsoft.intellij.util.ManualUpdateScheduler;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CsvTableMovementTest extends CsvTableEditorSwingTestBase {
    private static final String ORIGINAL = "a,b,c,d\n1,2,3,4\n5,6,7,8\n9,10,11,12\n";

    protected void configure(String text) {
        fileEditor.dispose();
        myFixture.configureByText(CsvFileType.INSTANCE, text);
        fileEditor = createEditor();
        fileEditor.selectNotify();
        fileEditor.getTable().setSize(800, 400);
        fileEditor.getTable().getTableHeader().setSize(800, 32);
    }

    private void select(int row, int column) {
        fileEditor.getTable().changeSelection(row, column, false, false);
    }

    public void testMoveSelectedColumnsAndKeepWidthsAndSelection() {
        configure(ORIGINAL);
        fileEditor.getTableEditorState().setColumnWidths(new int[]{80, 120, 160, 200});
        fileEditor.updateEditorLayout();
        select(1, 1);
        fileEditor.getTable().addColumnSelectionInterval(2, 2);
        fileEditor.moveSelection(true, false);
        assertEquals("a,d,b,c\n1,4,2,3\n5,8,6,7\n9,12,10,11\n", myFixture.getFile().getText());
        assertTrue(Arrays.equals(new int[]{80, 200, 120, 160}, fileEditor.getTableEditorState().getColumnWidths()));
        assertTrue(Arrays.equals(new int[]{2, 3}, fileEditor.getTable().getSelectedColumns()));
        assertEquals(1, fileEditor.getTable().getSelectedRow());
        fileEditor.moveSelection(true, true);
        assertEquals(ORIGINAL, myFixture.getFile().getText());
    }

    public void testMoveNonContiguousRowsBothDirectionsAndKeepSelection() {
        configure(ORIGINAL);
        select(1, 1);
        fileEditor.getTable().addRowSelectionInterval(3, 3);
        fileEditor.moveSelection(false, true);
        assertEquals("1,2,3,4\na,b,c,d\n9,10,11,12\n5,6,7,8\n", myFixture.getFile().getText());
        assertTrue(Arrays.equals(new int[]{0, 2}, fileEditor.getTable().getSelectedRows()));
        assertEquals(1, fileEditor.getTable().getSelectedColumn());
        fileEditor.moveSelection(false, false);
        assertEquals(ORIGINAL, myFixture.getFile().getText());
    }

    public void testMovementBoundariesAndNoSelection() {
        configure(ORIGINAL);
        fileEditor.getTable().clearSelection();
        assertFalse(fileEditor.canMoveSelection(true, false));
        assertFalse(fileEditor.canMoveSelection(false, true));
        select(0, 0);
        assertFalse(fileEditor.canMoveSelection(true, true));
        assertFalse(fileEditor.canMoveSelection(false, true));
        fileEditor.moveSelection(true, true);
        fileEditor.moveSelection(false, true);
        select(3, 3);
        assertFalse(fileEditor.canMoveSelection(true, false));
        assertFalse(fileEditor.canMoveSelection(false, false));
        select(4, 0);
        assertFalse(fileEditor.canMoveSelection(false, true));
        fileEditor.moveRow(4, 0);
        assertEquals(ORIGINAL, myFixture.getFile().getText());
    }

    public void testColumnHeaderDragPersistsAndRestoresIdentityMapping() {
        configure(ORIGINAL);
        dragColumn(0, 2);
        assertEquals("b,c,a,d\n2,3,1,4\n6,7,5,8\n10,11,9,12\n", myFixture.getFile().getText());
        for (int column = 0; column < 4; column++) {
            assertEquals(column, fileEditor.getTable().convertColumnIndexToModel(column));
        }
        assertEquals("b (1)", fileEditor.getTable().getColumnModel().getColumn(0).getHeaderValue());
        fileEditor.getTableModel().setValue("edited", 1, 0);
        assertEquals("edited", fileEditor.getTableModel().getValue(1, 0));
        dragColumn(2, 0);
        assertEquals("a,b,c,d\n1,edited,3,4\n5,6,7,8\n9,10,11,12\n", myFixture.getFile().getText());
    }

    public void testNativeColumnHeaderMouseDrag() {
        configure(ORIGINAL);
        nativeDragColumn(0, 2);
        assertEquals("b,c,a,d\n2,3,1,4\n6,7,5,8\n10,11,9,12\n", myFixture.getFile().getText());
    }

    public void testColumnHeaderDragMovesSelectedColumnsAndWidthsTogetherWithUndoRedo() {
        configure(ORIGINAL);
        fileEditor.getTableEditorState().setColumnWidths(new int[]{80, 120, 160, 200});
        fileEditor.updateEditorLayout();
        fileEditor.selectColumn(1, false);
        fileEditor.selectColumn(2, true);
        nativeDragColumn(1, 3);
        String moved = "a,d,b,c\n1,4,2,3\n5,8,6,7\n9,12,10,11\n";
        assertEquals(moved, myFixture.getFile().getText());
        assertColumnWidths(80, 200, 120, 160);
        assertOrderedEquals(fileEditor.getTable().getSelectedColumns(), new int[]{2, 3});
        UndoManager undo = UndoManager.getInstance(getProject());
        undo.undo(fileEditor);
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        assertEquals(ORIGINAL, myFixture.getFile().getText());
        assertColumnWidths(80, 120, 160, 200);
        assertOrderedEquals(fileEditor.getTable().getSelectedColumns(), new int[]{1, 2});
        undo.redo(fileEditor);
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        assertEquals(moved, myFixture.getFile().getText());
        assertColumnWidths(80, 200, 120, 160);
        assertOrderedEquals(fileEditor.getTable().getSelectedColumns(), new int[]{2, 3});
        for (int column = 0; column < 4; column++) {
            assertEquals(column, fileEditor.getTable().convertColumnIndexToModel(column));
        }
    }

    public void testColumnHeaderDragCollectsNonAdjacentColumnsInOriginalOrder() {
        configure(ORIGINAL);
        fileEditor.getTableEditorState().setColumnWidths(new int[]{80, 120, 160, 200});
        fileEditor.updateEditorLayout();
        JTableHeader header = fileEditor.getTable().getTableHeader();
        Rectangle first = header.getHeaderRect(1);
        Rectangle second = header.getHeaderRect(3);
        clickHeader(header, first.x + first.width / 2, 12, 0);
        clickHeader(header, second.x + second.width / 2, 12, InputEvent.CTRL_DOWN_MASK);
        assertOrderedEquals(fileEditor.getTable().getSelectedColumns(), new int[]{1, 3});
        nativeDragColumn(3, 0);
        assertEquals("b,d,a,c\n2,4,1,3\n6,8,5,7\n10,12,9,11\n", myFixture.getFile().getText());
        assertColumnWidths(120, 200, 80, 160);
        assertOrderedEquals(fileEditor.getTable().getSelectedColumns(), new int[]{0, 1});
        nativeDragColumn(0, 3);
        assertEquals("a,c,b,d\n1,3,2,4\n5,7,6,8\n9,11,10,12\n", myFixture.getFile().getText());
        assertColumnWidths(80, 160, 120, 200);
        assertOrderedEquals(fileEditor.getTable().getSelectedColumns(), new int[]{2, 3});
    }

    public void testColumnGroupDragCollectsColumnsFromBothSidesOfDropTarget() {
        configure("a,b,c,d,e,f,g\n1,2,3,4,5,6,7\n");
        fileEditor.getTableEditorState().setColumnWidths(new int[]{80, 90, 100, 110, 120, 130, 140});
        fileEditor.updateEditorLayout();
        fileEditor.selectColumn(0, false);
        fileEditor.selectColumn(1, true);
        fileEditor.selectColumn(5, true);
        fileEditor.selectColumn(6, true);
        nativeDragColumn(1, 3);
        assertEquals("c,d,a,b,f,g,e\n3,4,1,2,6,7,5\n", myFixture.getFile().getText());
        assertColumnWidths(100, 110, 80, 90, 130, 140, 120);
        assertOrderedEquals(fileEditor.getTable().getSelectedColumns(), new int[]{2, 3, 4, 5});
    }

    public void testColumnMovesWithoutMouseDragAlsoPersist() {
        configure(ORIGINAL);
        fileEditor.getTable().getColumnModel().moveColumn(0, 2);
        assertEquals("b,c,a,d\n2,3,1,4\n6,7,5,8\n10,11,9,12\n", myFixture.getFile().getText());
        assertEquals(0, fileEditor.getTable().convertColumnIndexToModel(0));
    }

    public void testModelRefreshCancelsStaleColumnDragWithoutPersistingIt() {
        configure(ORIGINAL);
        CsvTable table = fileEditor.getTable();
        JTableHeader header = table.getTableHeader();
        header.dispatchEvent(mouse(header, MouseEvent.MOUSE_PRESSED, 20, 12, MouseEvent.BUTTON1));
        table.getColumnModel().moveColumn(0, 2);
        table.getModel().updateTableImmediately();
        assertNull(header.getDraggedColumn());
        header.dispatchEvent(mouse(header, MouseEvent.MOUSE_DRAGGED, 300, 12, MouseEvent.NOBUTTON));
        header.dispatchEvent(mouse(header, MouseEvent.MOUSE_RELEASED, 300, 12, MouseEvent.BUTTON1));
        assertEquals(ORIGINAL, myFixture.getFile().getText());
        assertEquals(0, table.convertColumnIndexToModel(0));
    }

    private void nativeDragColumn(int source, int target) {
        JTableHeader header = fileEditor.getTable().getTableHeader();
        Rectangle from = header.getHeaderRect(source);
        Rectangle to = header.getHeaderRect(target);
        int x = from.x + from.width / 2;
        int targetX = to.x + to.width / 2;
        header.dispatchEvent(mouse(header, MouseEvent.MOUSE_PRESSED, x, 12, MouseEvent.BUTTON1));
        int step = x < targetX ? 5 : -5;
        for (int position = x + step; step > 0 ? position < targetX : position > targetX; position += step) {
            header.dispatchEvent(mouse(header, MouseEvent.MOUSE_DRAGGED, position, 12, MouseEvent.NOBUTTON));
        }
        header.dispatchEvent(mouse(header, MouseEvent.MOUSE_DRAGGED, targetX, 12, MouseEvent.NOBUTTON));
        header.dispatchEvent(mouse(header, MouseEvent.MOUSE_RELEASED, targetX, 12, MouseEvent.BUTTON1));
    }

    public void testNativeColumnDragWithFrozenColumnsWhileScrolled() {
        configure("a,b,c,d,e,f\n1,2,3,4,5,6\n");
        fileEditor.getTableEditorState().setColumnWidths(new int[]{100, 100, 100, 100, 100, 100});
        fileEditor.updateEditorLayout();
        fileEditor.setFrozenColumnCount(2);
        JViewport viewport = fileEditor.getTableScrollPane().getViewport();
        viewport.setExtentSize(new Dimension(400, 200));
        viewport.setViewPosition(new Point(150, 0));
        nativeDragColumn(0, 4);
        assertEquals("b,c,d,e,a,f\n2,3,4,5,1,6\n", myFixture.getFile().getText());
        assertEquals(2, fileEditor.getFrozenColumnCount());
        nativeDragColumn(4, 0);
        assertEquals("a,b,c,d,e,f\n1,2,3,4,5,6\n", myFixture.getFile().getText());
    }

    public void testColumnGroupDragCrossesFrozenBoundaryWithoutScrollingToFirstRow() {
        String original = "a,b,c,d,e,f\n1,2,3,4,5,6\n".repeat(20);
        configure(original);
        fileEditor.getTableEditorState().setColumnWidths(new int[]{100, 100, 100, 100, 100, 100});
        fileEditor.updateEditorLayout();
        fileEditor.setFrozenColumnCount(2);
        CsvTable table = fileEditor.getTable();
        table.setRowHeight(20);
        table.setSize(600, 1000);
        JViewport viewport = fileEditor.getTableScrollPane().getViewport();
        viewport.setExtentSize(new Dimension(400, 100));
        viewport.setViewPosition(new Point(150, 200));
        fileEditor.selectColumn(0, false);
        fileEditor.selectColumn(1, true);
        nativeDragColumn(1, 4);
        assertEquals(200, viewport.getViewPosition().y);
        assertEquals("c,d,e,a,b,f\n3,4,5,1,2,6\n".repeat(20), myFixture.getFile().getText());
        assertEquals(2, fileEditor.getFrozenColumnCount());
        assertOrderedEquals(table.getSelectedColumns(), new int[]{3, 4});
        nativeDragColumn(4, 0);
        assertEquals(original, myFixture.getFile().getText());
        assertEquals(2, fileEditor.getFrozenColumnCount());
        assertOrderedEquals(table.getSelectedColumns(), new int[]{0, 1});
    }

    public void testNativeColumnDragCommitsActiveCellEdit() {
        configure(ORIGINAL);
        select(1, 0);
        JTable table = fileEditor.getTable();
        assertTrue(table.editCellAt(1, 0));
        JTextArea text = (JTextArea) ((JScrollPane) table.getEditorComponent()).getViewport().getView();
        text.setText("changed");
        nativeDragColumn(0, 2);
        assertEquals("b,c,a,d\n2,3,changed,4\n6,7,5,8\n10,11,9,12\n", myFixture.getFile().getText());
    }

    public void testRejectedCellEditPreventsMovement() {
        configure(ORIGINAL);
        JTable table = fileEditor.getTable();
        table.setDefaultEditor(Object.class, new DefaultCellEditor(new JTextField()) {
            @Override
            public boolean stopCellEditing() {
                return false;
            }
        });
        select(1, 1);
        assertTrue(table.editCellAt(1, 1));
        fileEditor.moveSelection(true, true);
        fileEditor.moveRow(1, 2);
        nativeDragColumn(0, 2);
        assertEquals(ORIGINAL, myFixture.getFile().getText());
        assertTrue(table.isEditing());
        table.getCellEditor().cancelCellEditing();
    }

    public void testMoveCanBeUndoneAndRedoneFromTableEditor() {
        configure(ORIGINAL);
        fileEditor.getTableEditorState().setColumnWidths(new int[]{80, 200, 160, 240});
        fileEditor.updateEditorLayout();
        select(1, 1);
        fileEditor.moveSelection(true, true);
        String moved = myFixture.getFile().getText();
        assertColumnWidths(200, 80, 160, 240);
        assertEquals(1, fileEditor.getTable().getSelectedRow());
        assertEquals(0, fileEditor.getTable().getSelectedColumn());
        UndoManager undo = UndoManager.getInstance(getProject());
        assertTrue(undo.isUndoAvailable(fileEditor));
        undo.undo(fileEditor);
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        assertEquals(ORIGINAL, myFixture.getFile().getText());
        assertEquals("a", fileEditor.getTableModel().getValue(0, 0));
        assertColumnWidths(80, 200, 160, 240);
        assertEquals(1, fileEditor.getTable().getSelectedRow());
        assertEquals(1, fileEditor.getTable().getSelectedColumn());
        undo.redo(fileEditor);
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        assertEquals(moved, myFixture.getFile().getText());
        assertEquals("b", fileEditor.getTableModel().getValue(0, 0));
        assertColumnWidths(200, 80, 160, 240);
        assertEquals(1, fileEditor.getTable().getSelectedRow());
        assertEquals(0, fileEditor.getTable().getSelectedColumn());
    }

    private void assertColumnWidths(int... expected) {
        assertOrderedEquals(fileEditor.getTableEditorState().getColumnWidths(), expected);
        for (int column = 0; column < expected.length; column++) {
            assertEquals(expected[column], fileEditor.getTable().getColumnModel().getColumn(column).getWidth());
        }
    }

    public void testMoveUndoSnapshotsSurviveLaterLayoutAndSelectionChanges() {
        configure(ORIGINAL);
        fileEditor.getTableEditorState().setColumnWidths(new int[]{80, 200, 160, 240});
        fileEditor.updateEditorLayout();
        select(1, 1);
        fileEditor.moveSelection(true, true);
        fileEditor.getTableEditorState().getColumnWidths()[0] = 300;
        fileEditor.updateEditorLayout();
        select(2, 3);
        UndoManager undo = UndoManager.getInstance(getProject());
        for (int repeat = 0; repeat < 2; repeat++) {
            undo.undo(fileEditor);
            PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
            assertEquals(ORIGINAL, myFixture.getFile().getText());
            assertColumnWidths(80, 200, 160, 240);
            assertEquals(1, fileEditor.getTable().getSelectedRow());
            assertEquals(1, fileEditor.getTable().getSelectedColumn());
            undo.redo(fileEditor);
            PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
            assertColumnWidths(200, 80, 160, 240);
            assertEquals(1, fileEditor.getTable().getSelectedRow());
            assertEquals(0, fileEditor.getTable().getSelectedColumn());
        }
    }

    public void testColumnDragUndoRestoresWidthsAndSelectionAcrossFrozenBoundary() {
        configure(ORIGINAL);
        fileEditor.getTableEditorState().setColumnWidths(new int[]{80, 200, 160, 240});
        fileEditor.updateEditorLayout();
        fileEditor.setFrozenColumnCount(1);
        dragColumn(1, 0);
        assertColumnWidths(200, 80, 160, 240);
        UndoManager undo = UndoManager.getInstance(getProject());
        undo.undo(fileEditor);
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        assertEquals(ORIGINAL, myFixture.getFile().getText());
        assertColumnWidths(80, 200, 160, 240);
        assertOrderedEquals(fileEditor.getTable().getSelectedColumns(), new int[]{1});
        assertEquals(fileEditor.getTable().getRowCount(), fileEditor.getTable().getSelectedRowCount());
        assertEquals(1, fileEditor.getFrozenColumnCount());
        undo.redo(fileEditor);
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        assertColumnWidths(200, 80, 160, 240);
        assertOrderedEquals(fileEditor.getTable().getSelectedColumns(), new int[]{0});
        assertEquals(1, fileEditor.getFrozenColumnCount());
    }

    public void testRowMoveUndoRestoresNonContiguousSelection() {
        configure(ORIGINAL);
        select(1, 2);
        fileEditor.getTable().addRowSelectionInterval(3, 3);
        fileEditor.moveSelection(false, true);
        UndoManager undo = UndoManager.getInstance(getProject());
        undo.undo(fileEditor);
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        assertEquals(ORIGINAL, myFixture.getFile().getText());
        assertOrderedEquals(fileEditor.getTable().getSelectedRows(), new int[]{1, 3});
        assertOrderedEquals(fileEditor.getTable().getSelectedColumns(), new int[]{2});
        undo.redo(fileEditor);
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        assertOrderedEquals(fileEditor.getTable().getSelectedRows(), new int[]{0, 2});
        assertOrderedEquals(fileEditor.getTable().getSelectedColumns(), new int[]{2});
    }

    public void testMoveUndoAndRedoKeepLayoutAndSelectionAfterDelayedRefresh() {
        ManualUpdateScheduler scheduler = configureDelayed();
        fileEditor.getTableEditorState().setColumnWidths(new int[]{80, 200, 160, 240});
        fileEditor.updateEditorLayout();
        select(1, 1);
        fileEditor.moveSelection(true, true);
        UndoManager undo = UndoManager.getInstance(getProject());
        undo.undo(fileEditor);
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        scheduler.advanceBy(50);
        assertColumnWidths(80, 200, 160, 240);
        assertEquals(1, fileEditor.getTable().getSelectedColumn());
        undo.redo(fileEditor);
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        scheduler.advanceBy(50);
        assertColumnWidths(200, 80, 160, 240);
        assertEquals(0, fileEditor.getTable().getSelectedColumn());
    }

    public void testUndoFromTextEditorAlsoRestoresTableLayout() {
        configure(ORIGINAL);
        fileEditor.getTableEditorState().setColumnWidths(new int[]{80, 200, 160, 240});
        fileEditor.updateEditorLayout();
        select(1, 1);
        fileEditor.moveSelection(true, true);
        var textEditor = TextEditorProvider.getInstance().getTextEditor(myFixture.getEditor());
        UndoManager undo = UndoManager.getInstance(getProject());
        undo.undo(textEditor);
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        assertEquals(ORIGINAL, myFixture.getFile().getText());
        assertColumnWidths(80, 200, 160, 240);
        undo.redo(textEditor);
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        assertColumnWidths(200, 80, 160, 240);
    }

    public void testMovementPersistsWhenEditorIsReopened() {
        configure(ORIGINAL);
        fileEditor.moveRow(1, 3);
        fileEditor.selectColumn(0, false);
        nativeDragColumn(0, 2);
        String moved = myFixture.getFile().getText();
        fileEditor.dispose();
        fileEditor = createEditor();
        fileEditor.selectNotify();
        assertEquals(moved, fileEditor.getPsiFile().getText());
        assertEquals("b", fileEditor.getTableModel().getValue(0, 0));
        assertEquals("2", fileEditor.getTableModel().getValue(3, 0));
    }

    public void testRowNumberDragPersistsAndSelectsMovedRow() {
        configure(ORIGINAL);
        dragRow(1, 3);
        assertEquals("a,b,c,d\n5,6,7,8\n9,10,11,12\n1,2,3,4\n", myFixture.getFile().getText());
        assertTrue(Arrays.equals(new int[]{3}, fileEditor.getTable().getSelectedRows()));
        assertEquals(4, fileEditor.getTable().getSelectedColumnCount());
        dragRow(3, 1);
        assertEquals(ORIGINAL, myFixture.getFile().getText());
    }

    public void testRowNumberDragMovesSelectedRowsTogetherAndSupportsUndoRedo() {
        configure(ORIGINAL);
        JTable headers = rowHeaders();
        headers.setRowSelectionInterval(1, 2);
        dragRow(1, 3);
        String moved = "a,b,c,d\n9,10,11,12\n1,2,3,4\n5,6,7,8\n";
        assertEquals(moved, myFixture.getFile().getText());
        assertOrderedEquals(fileEditor.getTable().getSelectedRows(), new int[]{2, 3});
        assertEquals(4, fileEditor.getTable().getSelectedColumnCount());
        UndoManager undo = UndoManager.getInstance(getProject());
        undo.undo(fileEditor);
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        assertEquals(ORIGINAL, myFixture.getFile().getText());
        assertOrderedEquals(fileEditor.getTable().getSelectedRows(), new int[]{1, 2});
        undo.redo(fileEditor);
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        assertEquals(moved, myFixture.getFile().getText());
        assertOrderedEquals(fileEditor.getTable().getSelectedRows(), new int[]{2, 3});
    }

    public void testRowNumberDragCollectsNonAdjacentRowsInOriginalOrder() {
        configure(ORIGINAL);
        JTable headers = rowHeaders();
        clickHeader(headers, 5, headers.getCellRect(1, 0, true).y + headers.getRowHeight() / 2, 0);
        clickHeader(headers, 5, headers.getCellRect(3, 0, true).y + headers.getRowHeight() / 2, InputEvent.CTRL_DOWN_MASK);
        assertOrderedEquals(fileEditor.getTable().getSelectedRows(), new int[]{1, 3});
        dragRow(3, 0);
        assertEquals("1,2,3,4\n9,10,11,12\na,b,c,d\n5,6,7,8\n", myFixture.getFile().getText());
        assertOrderedEquals(fileEditor.getTable().getSelectedRows(), new int[]{0, 1});
        dragRow(0, 3);
        assertEquals("a,b,c,d\n5,6,7,8\n1,2,3,4\n9,10,11,12\n", myFixture.getFile().getText());
        assertOrderedEquals(fileEditor.getTable().getSelectedRows(), new int[]{2, 3});
    }

    public void testRowGroupDragPreservesCommentsMultilineValuesAndTerminalEmptyRow() {
        configure("#first\na,\"multi\nline\"\n#last\nb,value\n");
        JTable headers = rowHeaders();
        headers.setRowSelectionInterval(0, 1);
        headers.addRowSelectionInterval(4, 4);
        int fromY = headers.getCellRect(0, 0, true).y + headers.getRowHeight() / 2;
        int toY = headers.getCellRect(4, 0, true).y + headers.getRowHeight() / 2;
        headers.dispatchEvent(mouse(headers, MouseEvent.MOUSE_PRESSED, 5, fromY, MouseEvent.BUTTON1));
        headers.dispatchEvent(mouse(headers, MouseEvent.MOUSE_DRAGGED, 5, toY, MouseEvent.NOBUTTON));
        assertEquals("Moving 2 rows", headers.getAccessibleContext().getAccessibleDescription());
        headers.dispatchEvent(mouse(headers, MouseEvent.MOUSE_RELEASED, 5, toY, MouseEvent.BUTTON1));
        assertEquals("#last\nb,value\n#first\na,\"multi\nline\"\n", myFixture.getFile().getText());
        assertOrderedEquals(fileEditor.getTable().getSelectedRows(), new int[]{2, 3});
        assertEquals(4, fileEditor.getTableModel().getMovableRowCount());
        assertEquals(5, fileEditor.getTable().getRowCount());
    }

    public void testDraggingGroupOntoSelectedItemDoesNotReorderOrLoseSelection() {
        configure(ORIGINAL);
        JTable headers = rowHeaders();
        headers.setRowSelectionInterval(1, 2);
        dragRow(1, 2);
        assertEquals(ORIGINAL, myFixture.getFile().getText());
        assertOrderedEquals(fileEditor.getTable().getSelectedRows(), new int[]{1, 2});
        fileEditor.selectColumn(1, false);
        fileEditor.selectColumn(2, true);
        nativeDragColumn(1, 2);
        assertEquals(ORIGINAL, myFixture.getFile().getText());
        assertOrderedEquals(fileEditor.getTable().getSelectedColumns(), new int[]{1, 2});
    }

    public void testClickingSelectedHeaderWithoutDraggingStillSelectsOneItem() {
        configure(ORIGINAL);
        JTable headers = rowHeaders();
        headers.setRowSelectionInterval(1, 2);
        clickHeader(headers, 5, headers.getCellRect(1, 0, true).y + headers.getRowHeight() / 2, 0);
        assertOrderedEquals(fileEditor.getTable().getSelectedRows(), new int[]{1});
        fileEditor.selectColumn(1, false);
        fileEditor.selectColumn(2, true);
        JTableHeader header = fileEditor.getTable().getTableHeader();
        Rectangle column = header.getHeaderRect(1);
        clickHeader(header, column.x + column.width / 2, 12, 0);
        assertOrderedEquals(fileEditor.getTable().getSelectedColumns(), new int[]{1});
        assertEquals(ORIGINAL, myFixture.getFile().getText());
    }

    public void testReadOnlyHeaderClickStillSelectsOneColumn() {
        configure(ORIGINAL);
        fileEditor.setEditable(false);
        fileEditor.selectColumn(1, false);
        fileEditor.selectColumn(2, true);
        JTableHeader header = fileEditor.getTable().getTableHeader();
        Rectangle column = header.getHeaderRect(1);
        clickHeader(header, column.x + column.width / 2, 12, 0);
        assertOrderedEquals(fileEditor.getTable().getSelectedColumns(), new int[]{1});
        assertEquals(ORIGINAL, myFixture.getFile().getText());
    }

    public void testModelRefreshCancelsColumnGroupDragWithoutPersistingIt() {
        ManualUpdateScheduler scheduler = configureDelayed();
        fileEditor.selectColumn(1, false);
        fileEditor.selectColumn(3, true);
        JTableHeader header = fileEditor.getTable().getTableHeader();
        Rectangle source = header.getHeaderRect(1);
        Rectangle target = header.getHeaderRect(2);
        int fromX = source.x + source.width / 2;
        int toX = target.x + target.width / 2;
        header.dispatchEvent(mouse(header, MouseEvent.MOUSE_PRESSED, fromX, 12, MouseEvent.BUTTON1));
        header.dispatchEvent(mouse(header, MouseEvent.MOUSE_DRAGGED, toX, 12, MouseEvent.NOBUTTON));
        assertNotNull(header.getDraggedColumn());
        assertOrderedEquals(fileEditor.getTable().getSelectedColumns(), new int[]{1, 3});
        String latest = ORIGINAL.replace("1,2,3,4", "edited,2,3,4");
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            myFixture.getEditor().getDocument().setText(latest);
            PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        });
        scheduler.advanceBy(50);
        assertNull(header.getDraggedColumn());
        header.dispatchEvent(mouse(header, MouseEvent.MOUSE_RELEASED, toX, 12, MouseEvent.BUTTON1));
        assertEquals(latest, myFixture.getFile().getText());
        for (int column = 0; column < 4; column++) {
            assertEquals(column, fileEditor.getTable().convertColumnIndexToModel(column));
        }
    }

    public void testEscapeCancelsRowGroupDragWithoutLosingSelection() {
        configure(ORIGINAL);
        JTable headers = rowHeaders();
        headers.setRowSelectionInterval(1, 1);
        headers.addRowSelectionInterval(3, 3);
        int fromY = headers.getCellRect(1, 0, true).y + headers.getRowHeight() / 2;
        int toY = headers.getCellRect(2, 0, true).y + headers.getRowHeight() / 2;
        headers.dispatchEvent(mouse(headers, MouseEvent.MOUSE_PRESSED, 5, fromY, MouseEvent.BUTTON1));
        headers.dispatchEvent(mouse(headers, MouseEvent.MOUSE_DRAGGED, 5, toY, MouseEvent.NOBUTTON));
        assertEquals("Moving 2 rows", headers.getAccessibleContext().getAccessibleDescription());
        headers.getActionMap().get("csv.cancel.row.drag").actionPerformed(null);
        assertNull(headers.getAccessibleContext().getAccessibleDescription());
        assertFalse(fileEditor.getTable().getSelectionModel().getValueIsAdjusting());
        headers.dispatchEvent(mouse(headers, MouseEvent.MOUSE_RELEASED, 5, toY, MouseEvent.BUTTON1));
        assertEquals(ORIGINAL, myFixture.getFile().getText());
        assertOrderedEquals(fileEditor.getTable().getSelectedRows(), new int[]{1, 3});
    }

    public void testDelayedRefreshCancelsPressedAndDraggingRowsWithoutMovingThem() {
        for (boolean dragging : new boolean[]{false, true}) {
            ManualUpdateScheduler scheduler = configureDelayed();
            CsvTable table = fileEditor.getTable();
            JTable headers = rowHeaders();
            int fromY = headers.getCellRect(1, 0, true).y + headers.getRowHeight() / 2;
            int toY = headers.getCellRect(3, 0, true).y + headers.getRowHeight() / 2;
            headers.dispatchEvent(mouse(headers, MouseEvent.MOUSE_PRESSED, 5, fromY, MouseEvent.BUTTON1));
            if (dragging) {
                headers.dispatchEvent(mouse(headers, MouseEvent.MOUSE_DRAGGED, 5, toY, MouseEvent.NOBUTTON));
                assertEquals("Moving row 2", headers.getAccessibleContext().getAccessibleDescription());
                assertTrue(table.getSelectionModel().getValueIsAdjusting());
            }
            String latest = ORIGINAL.replace("1,2,3,4", "edited,2,3,4");
            WriteCommandAction.runWriteCommandAction(getProject(), () -> {
                myFixture.getEditor().getDocument().setText(latest);
                PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
            });
            assertEquals(1, scheduler.pendingCount());
            scheduler.advanceBy(49);
            assertTrue(headers.getActionMap().get("csv.cancel.row.drag").isEnabled());
            scheduler.advanceBy(1);
            assertFalse(headers.getActionMap().get("csv.cancel.row.drag").isEnabled());
            assertNull(headers.getAccessibleContext().getAccessibleDescription());
            assertFalse(table.getSelectionModel().getValueIsAdjusting());
            headers.dispatchEvent(mouse(headers, MouseEvent.MOUSE_RELEASED, 5, toY, MouseEvent.BUTTON1));
            assertEquals(latest, myFixture.getFile().getText());
            assertEquals("edited", getTableModel().getValue(1, 0));
        }
    }

    public void testDisposingEditorCancelsDragAndPendingRefresh() {
        ManualUpdateScheduler scheduler = configureDelayed();
        CsvTable table = fileEditor.getTable();
        JTable headers = rowHeaders();
        int fromY = headers.getCellRect(1, 0, true).y + headers.getRowHeight() / 2;
        int toY = headers.getCellRect(3, 0, true).y + headers.getRowHeight() / 2;
        headers.dispatchEvent(mouse(headers, MouseEvent.MOUSE_PRESSED, 5, fromY, MouseEvent.BUTTON1));
        headers.dispatchEvent(mouse(headers, MouseEvent.MOUSE_DRAGGED, 5, toY, MouseEvent.NOBUTTON));
        assertTrue(table.getSelectionModel().getValueIsAdjusting());
        AtomicInteger updates = new AtomicInteger();
        table.getModel().addTableModelListener(event -> updates.incrementAndGet());
        table.getModel().notifyUpdate();
        Runnable queued = scheduler.pendingUpdate();
        fileEditor.dispose();
        assertEquals(0, scheduler.pendingCount());
        scheduler.advanceBy(50);
        queued.run();
        assertEquals(0, updates.get());
        assertFalse(headers.getActionMap().get("csv.cancel.row.drag").isEnabled());
        assertFalse(table.getSelectionModel().getValueIsAdjusting());
        assertNull(headers.getAccessibleContext().getAccessibleDescription());
        headers.dispatchEvent(mouse(headers, MouseEvent.MOUSE_RELEASED, 5, toY, MouseEvent.BUTTON1));
        assertEquals(ORIGINAL, myFixture.getFile().getText());
    }

    public void testImmediateRefreshCancelsPendingDelayedDelivery() {
        ManualUpdateScheduler scheduler = configureDelayed();
        CsvTableModelSwing model = fileEditor.getTable().getModel();
        AtomicInteger updates = new AtomicInteger();
        model.addTableModelListener(event -> updates.incrementAndGet());
        model.notifyUpdate();
        assertEquals(1, scheduler.pendingCount());
        model.updateTableImmediately();
        assertEquals(1, updates.get());
        assertEquals(0, scheduler.pendingCount());
        scheduler.advanceBy(50);
        assertEquals(1, updates.get());
    }

    public void testSuspendedEditorIgnoresDeliveryAndRefreshesWhenSelectedAgain() {
        ManualUpdateScheduler scheduler = configureDelayed();
        CsvTableModelSwing model = fileEditor.getTable().getModel();
        AtomicInteger updates = new AtomicInteger();
        model.addTableModelListener(event -> updates.incrementAndGet());
        model.notifyUpdate();
        fileEditor.deselectNotify();
        scheduler.advanceBy(50);
        assertEquals(0, updates.get());
        assertEquals(0, scheduler.pendingCount());
        fileEditor.selectNotify();
        assertEquals(1, scheduler.pendingCount());
        scheduler.advanceBy(50);
        assertEquals(1, updates.get());
        assertEquals(0, scheduler.pendingCount());
    }

    public void testRowDragBelowThresholdDoesNotMoveOnReleaseAfterRefreshWasScheduled() {
        ManualUpdateScheduler scheduler = configureDelayed();
        JTable headers = rowHeaders();
        int fromY = headers.getCellRect(1, 0, true).y + headers.getRowHeight() / 2;
        int toY = headers.getCellRect(3, 0, true).y + headers.getRowHeight() / 2;
        fileEditor.getTableModel().notifyUpdate();
        headers.dispatchEvent(mouse(headers, MouseEvent.MOUSE_PRESSED, 5, fromY, MouseEvent.BUTTON1));
        headers.dispatchEvent(mouse(headers, MouseEvent.MOUSE_DRAGGED, 5, fromY, MouseEvent.NOBUTTON));
        assertNull(headers.getAccessibleContext().getAccessibleDescription());
        headers.dispatchEvent(mouse(headers, MouseEvent.MOUSE_RELEASED, 5, toY, MouseEvent.BUTTON1));
        scheduler.advanceBy(50);
        assertEquals(ORIGINAL, myFixture.getFile().getText());
        assertFalse(headers.getActionMap().get("csv.cancel.row.drag").isEnabled());
    }

    public void testRowDragCancelsWhenReadOnlyBeforeDelayedRefresh() {
        ManualUpdateScheduler scheduler = configureDelayed();
        CsvTable table = fileEditor.getTable();
        JTable headers = rowHeaders();
        int fromY = headers.getCellRect(1, 0, true).y + headers.getRowHeight() / 2;
        int toY = headers.getCellRect(3, 0, true).y + headers.getRowHeight() / 2;
        headers.dispatchEvent(mouse(headers, MouseEvent.MOUSE_PRESSED, 5, fromY, MouseEvent.BUTTON1));
        headers.dispatchEvent(mouse(headers, MouseEvent.MOUSE_DRAGGED, 5, toY, MouseEvent.NOBUTTON));
        assertTrue(table.getSelectionModel().getValueIsAdjusting());
        table.getModel().notifyUpdate();
        fileEditor.setEditable(false);
        headers.dispatchEvent(mouse(headers, MouseEvent.MOUSE_DRAGGED, 5, toY, MouseEvent.NOBUTTON));
        assertFalse(table.getSelectionModel().getValueIsAdjusting());
        assertNull(headers.getAccessibleContext().getAccessibleDescription());
        assertFalse(headers.getActionMap().get("csv.cancel.row.drag").isEnabled());
        headers.dispatchEvent(mouse(headers, MouseEvent.MOUSE_RELEASED, 5, toY, MouseEvent.BUTTON1));
        scheduler.advanceBy(50);
        assertEquals(ORIGINAL, myFixture.getFile().getText());
    }

    private ManualUpdateScheduler configureDelayed() {
        ManualUpdateScheduler scheduler = new ManualUpdateScheduler();
        updateScheduler = scheduler;
        configure(ORIGINAL);
        scheduler.advanceBy(50);
        assertEquals(0, scheduler.pendingCount());
        return scheduler;
    }

    public void testRowDragPreviewShowsSourceCellsAndFadesAt400Pixels() {
        configureRowPreview();
        BufferedImage preview = rowPreview(1);
        assertEquals(JBUI.scale(400), preview.getWidth());
        int y = preview.getHeight() / 2;
        assertEquals(Color.RED.getRGB(), preview.getRGB(JBUI.scale(60), y));
        assertEquals(Color.GREEN.getRGB(), preview.getRGB(JBUI.scale(160), y));
        assertEquals(Color.BLUE.getRGB(), preview.getRGB(JBUI.scale(260), y));
        int opaque = preview.getRGB(preview.getWidth() - JBUI.scale(65), y) >>> 24;
        int fading = preview.getRGB(preview.getWidth() - JBUI.scale(32), y) >>> 24;
        int edge = preview.getRGB(preview.getWidth() - 1, y) >>> 24;
        assertEquals(255, opaque);
        assertTrue(fading > 0 && fading < opaque);
        assertTrue(edge < fading);
        assertTrue(edge < 10);
    }

    public void testRowDragPreviewUsesCurrentlyVisibleAndFrozenColumns() {
        configureRowPreview();
        fileEditor.getTableScrollPane().getViewport().setViewPosition(new Point(JBUI.scale(230), 0));
        assertEquals(JBUI.scale(230), fileEditor.getTableScrollPane().getViewport().getViewPosition().x);
        BufferedImage scrolled = rowPreview(1);
        int y = scrolled.getHeight() / 2;
        assertEquals(Color.BLUE.getRGB(), scrolled.getRGB(JBUI.scale(60), y));
        assertEquals(Color.CYAN.getRGB(), scrolled.getRGB(JBUI.scale(160), y));
        fileEditor.setFrozenColumnCount(2);
        BufferedImage frozen = rowPreview(1);
        assertEquals(Color.RED.getRGB(), frozen.getRGB(JBUI.scale(60), y));
        assertEquals(Color.GREEN.getRGB(), frozen.getRGB(JBUI.scale(160), y));
        assertEquals(Color.MAGENTA.getRGB(), frozen.getRGB(JBUI.scale(260), y));
    }

    public void testRowDragPreviewBoundsTallRowsAndNarrowViewports() {
        configureRowPreview();
        fileEditor.getTable().setRowHeight(1, JBUI.scale(200));
        BufferedImage tall = rowPreview(1);
        assertEquals(JBUI.scale(56), tall.getHeight());
        JViewport viewport = fileEditor.getTableScrollPane().getViewport();
        viewport.setSize(JBUI.scale(180), JBUI.scale(200));
        BufferedImage narrow = rowPreview(1);
        assertEquals(JBUI.scale(180) - JBUI.scale(6) * 2, narrow.getWidth());
        assertEquals(Color.RED.getRGB(), narrow.getRGB(JBUI.scale(60), narrow.getHeight() / 2));
    }

    public void testRowDragPreviewPreservesSpanningComments() {
        configure("#comment\na,b,c,d,e,f\n1,2,3,4,5,6\n");
        layoutRowPreview();
        CsvTable table = fileEditor.getTable();
        List<Object> rendered = new ArrayList<>();
        table.setDefaultRenderer(CsvTable.CommentColumn.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                           boolean focused, int row, int column) {
                rendered.add(value);
                super.getTableCellRendererComponent(table, "", false, false, row, column);
                setBackground(Color.ORANGE);
                return this;
            }
        });
        BufferedImage preview = rowPreview(0);
        assertFalse(rendered.isEmpty());
        assertTrue(rendered.stream().allMatch("comment"::equals));
        int y = preview.getHeight() / 2;
        assertEquals(Color.ORANGE.getRGB(), preview.getRGB(JBUI.scale(60), y));
        assertEquals(Color.ORANGE.getRGB(), preview.getRGB(JBUI.scale(260), y));
    }

    public void testRowDragCachesPreviewAndEscapeClearsFeedbackWithoutMoving() {
        configureRowPreview();
        List<Object> rendered = new ArrayList<>();
        fileEditor.getTable().setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                           boolean focused, int row, int column) {
                rendered.add(value);
                return super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            }
        });
        String original = myFixture.getFile().getText();
        JTable headers = rowHeaders();
        int fromY = headers.getCellRect(1, 0, true).y + headers.getRowHeight() / 2;
        int toY = headers.getCellRect(2, 0, true).y + headers.getRowHeight() / 2;
        headers.dispatchEvent(mouse(headers, MouseEvent.MOUSE_PRESSED, 5, fromY, MouseEvent.BUTTON1));
        headers.dispatchEvent(mouse(headers, MouseEvent.MOUSE_DRAGGED, 5, toY, MouseEvent.NOBUTTON));
        assertTrue(rendered.containsAll(List.of("1", "2", "3", "4")));
        assertFalse(rendered.contains("a"));
        assertEquals("Moving row 2", headers.getAccessibleContext().getAccessibleDescription());
        int count = rendered.size();
        CsvTable table = fileEditor.getTable();
        CsvTableRowDragListener listener = (CsvTableRowDragListener) table.getClientProperty(CsvTableRowDragListener.class);
        BufferedImage feedback = new BufferedImage(table.getWidth(), table.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = feedback.createGraphics();
        try {
            listener.paintFeedback(g, table);
            listener.paintFeedback(g, table);
            assertEquals(count, rendered.size());
            assertTrue(Arrays.stream(feedback.getRGB(0, 0, feedback.getWidth(), feedback.getHeight(), null, 0,
                    feedback.getWidth())).anyMatch(pixel -> pixel != 0));
            headers.getActionMap().get("csv.cancel.row.drag").actionPerformed(null);
            g.setComposite(AlphaComposite.Clear);
            g.fillRect(0, 0, feedback.getWidth(), feedback.getHeight());
            g.setComposite(AlphaComposite.SrcOver);
            listener.paintFeedback(g, table);
            assertTrue(Arrays.stream(feedback.getRGB(0, 0, feedback.getWidth(), feedback.getHeight(), null, 0,
                    feedback.getWidth())).allMatch(pixel -> pixel == 0));
        } finally {
            g.dispose();
        }
        assertNull(headers.getAccessibleContext().getAccessibleDescription());
        headers.dispatchEvent(mouse(headers, MouseEvent.MOUSE_RELEASED, 5, toY, MouseEvent.BUTTON1));
        assertEquals(original, myFixture.getFile().getText());
    }

    private void configureRowPreview() {
        configure("a,b,c,d,e,f,g,h\n1,2,3,4,5,6,7,8\n9,10,11,12,13,14,15,16\n");
        layoutRowPreview();
        Color[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.ORANGE, Color.PINK};
        fileEditor.getTable().setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                           boolean focused, int row, int column) {
                super.getTableCellRendererComponent(table, "", false, false, row, column);
                setBackground(row == 1 ? colors[column] : Color.GRAY);
                return this;
            }
        });
    }

    private void layoutRowPreview() {
        int[] widths = new int[fileEditor.getTable().getColumnCount()];
        Arrays.fill(widths, JBUI.scale(100));
        fileEditor.getTableEditorState().setColumnWidths(widths);
        fileEditor.updateEditorLayout();
        JPanel root = new JPanel(new BorderLayout());
        root.add(fileEditor.getTableScrollPane());
        root.setSize(JBUI.scale(550), JBUI.scale(300));
        layoutRecursively(root);
        fileEditor.getTable().setSize(fileEditor.getTable().getWidth(), JBUI.scale(400));
    }

    private static void layoutRecursively(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) layoutRecursively(nested);
        }
    }

    private BufferedImage rowPreview(int row) {
        return ImageUtil.toBufferedImage(CsvTableRowDragListener.createDragPreview(fileEditor.getTable(), row), true);
    }

    public void testRowHeaderSelectionReplacesOldRowsAndSelectsAllColumns() {
        configure(ORIGINAL);
        select(0, 1);
        JTable headers = rowHeaders();
        headers.setRowSelectionInterval(2, 2);
        assertTrue(Arrays.equals(new int[]{2}, fileEditor.getTable().getSelectedRows()));
        assertEquals(4, fileEditor.getTable().getSelectedColumnCount());
    }

    public void testDraggingRowBoundaryStillResizesWithoutMoving() {
        configure(ORIGINAL);
        JTable headers = rowHeaders();
        int y = headers.getRowHeight() - 1;
        headers.dispatchEvent(mouse(headers, MouseEvent.MOUSE_PRESSED, 5, y, MouseEvent.BUTTON1));
        headers.dispatchEvent(mouse(headers, MouseEvent.MOUSE_DRAGGED, 5, y + 10, MouseEvent.NOBUTTON));
        headers.dispatchEvent(mouse(headers, MouseEvent.MOUSE_RELEASED, 5, y + 10, MouseEvent.BUTTON1));
        assertEquals(ORIGINAL, myFixture.getFile().getText());
        assertEquals(fileEditor.getTable().getRowHeight(), headers.getRowHeight());
    }

    public void testMovementCommitsActiveCellEdit() {
        configure(ORIGINAL);
        select(1, 1);
        JTable table = fileEditor.getTable();
        assertTrue(table.editCellAt(1, 1));
        JTextArea text = (JTextArea) ((JScrollPane) table.getEditorComponent()).getViewport().getView();
        text.setText("changed");
        fileEditor.moveSelection(true, true);
        assertEquals("b,a,c,d\nchanged,1,3,4\n6,5,7,8\n10,9,11,12\n", myFixture.getFile().getText());
        assertFalse(table.isEditing());
    }

    public void testMovementWithFrozenColumnsAndAfterTextEditorChange() {
        configure(ORIGINAL);
        fileEditor.setFrozenColumnCount(2);
        dragColumn(0, 3);
        assertEquals(2, fileEditor.getFrozenColumnCount());
        assertEquals("b,c,d,a\n2,3,4,1\n6,7,8,5\n10,11,12,9\n", myFixture.getFile().getText());
        WriteAction.run(() -> myFixture.getEditor().getDocument().setText("x,y,z\n1,2,3\n"));
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        select(1, 2);
        fileEditor.moveSelection(true, true);
        assertEquals("x,z,y\n1,3,2\n", myFixture.getFile().getText());
    }

    public void testReadOnlyAndErroneousFilesCannotMove() throws Exception {
        configure(ORIGINAL);
        WriteAction.run(() -> fileEditor.getFile().setWritable(false));
        try {
            select(1, 1);
            assertFalse(fileEditor.canMoveSelection(true, true));
            fileEditor.moveSelection(true, true);
            fileEditor.moveRow(1, 2);
            assertEquals(ORIGINAL, myFixture.getFile().getText());
        } finally {
            WriteAction.run(() -> fileEditor.getFile().setWritable(true));
        }
        configure("a,b\n\"invalid");
        assertFalse(fileEditor.getTable().getTableHeader().getReorderingAllowed());
        select(0, 1);
        assertFalse(fileEditor.canMoveSelection(true, true));
        fileEditor.moveSelection(true, true);
        fileEditor.moveRow(0, 1);
        assertEquals("a,b\n\"invalid", myFixture.getFile().getText());
    }

    public void testRegisteredMovementActionsUseSelectionAndDisableAtBoundaries() {
        configure(ORIGINAL);
        String[] names = {"MoveRowsUp", "MoveRowsDown", "MoveColumnsLeft", "MoveColumnsRight"};
        for (String name : names) {
            select(1, 1);
            AnAction action = ActionManager.getInstance().getAction(CsvTableEditorActions.class.getName() + "$" + name);
            assertNotNull(action);
            DataContext context = SimpleDataContext.getSimpleContext(PlatformDataKeys.FILE_EDITOR, fileEditor);
            AnActionEvent event = AnActionEvent.createEvent(action, context, null, ActionPlaces.UNKNOWN, ActionUiKind.NONE, null);
            action.update(event);
            assertTrue(event.getPresentation().isEnabled());
            assertEquals(ActionUpdateThread.EDT, action.getActionUpdateThread());
            action.actionPerformed(event);
            assertFalse(ORIGINAL.equals(myFixture.getFile().getText()));
            configure(ORIGINAL);
            select(name.equals("MoveRowsUp") ? 0 : 3, name.equals("MoveColumnsLeft") ? 0 : 3);
            event = event.withDataContext(SimpleDataContext.getSimpleContext(PlatformDataKeys.FILE_EDITOR, fileEditor));
            action.update(event);
            assertFalse(event.getPresentation().isEnabled());
        }
    }

    private void dragColumn(int from, int to) {
        JTable table = fileEditor.getTable();
        fileEditor.selectColumn(from, false);
        table.getTableHeader().setDraggedColumn(table.getColumnModel().getColumn(from));
        table.getColumnModel().moveColumn(from, to);
        table.getTableHeader().setDraggedColumn(null);
    }

    private JTable rowHeaders() {
        JTable table = (JTable) fileEditor.getTableScrollPane().getRowHeader().getView();
        table.setSize(40, 400);
        return table;
    }

    private void dragRow(int from, int to) {
        JTable headers = rowHeaders();
        int y = headers.getCellRect(from, 0, true).y + headers.getRowHeight() / 2;
        int targetY = headers.getCellRect(to, 0, true).y + headers.getRowHeight() / 2;
        headers.dispatchEvent(mouse(headers, MouseEvent.MOUSE_PRESSED, 5, y, MouseEvent.BUTTON1));
        headers.dispatchEvent(mouse(headers, MouseEvent.MOUSE_DRAGGED, 5, targetY, MouseEvent.NOBUTTON));
        headers.dispatchEvent(mouse(headers, MouseEvent.MOUSE_RELEASED, 5, targetY, MouseEvent.BUTTON1));
    }

    private MouseEvent mouse(Component component, int id, int x, int y, int button) {
        return new MouseEvent(component, id, System.currentTimeMillis(),
                id == MouseEvent.MOUSE_RELEASED ? 0 : InputEvent.BUTTON1_DOWN_MASK,
                x, y, 1, false, button);
    }

    private void clickHeader(Component header, int x, int y, int modifiers) {
        header.dispatchEvent(new MouseEvent(header, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                modifiers | InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1));
        header.dispatchEvent(new MouseEvent(header, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(),
                modifiers, x, y, 1, false, MouseEvent.BUTTON1));
    }
}
