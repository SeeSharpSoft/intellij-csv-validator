package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;

public class CsvMultiLineCellRendererTest extends CsvTableEditorSwingTestBase {

    public void testCellRendererUsage() {
        TableCellRenderer cellRenderer = fileEditor.getTable().getCellRenderer(0, 0);

        assertInstanceOf(cellRenderer, CsvMultiLineCellRenderer.class);
    }

    public void testPreferredSize() {
        JScrollPane jTextArea = (JScrollPane) fileEditor.getTable().getCellRenderer(0, 0);

        assertNotNull(jTextArea.getPreferredSize());
    }

    public void testCellEditorComponent() {
        CsvMultiLineCellRenderer cellRenderer = (CsvMultiLineCellRenderer) fileEditor.getTable().getCellRenderer(0, 0);

        assertEquals(cellRenderer, cellRenderer.getTableCellEditorComponent(fileEditor.getTable(), "Test", true, 0, 0));
    }

    public void testFocusedEditableCellUsesTableColors() {
        JTable table = fileEditor.getTable();
        Color foreground = Color.MAGENTA;
        Color background = Color.ORANGE;
        table.setForeground(foreground);
        table.setBackground(background);

        CsvMultiLineCellRenderer renderer = (CsvMultiLineCellRenderer) table.getCellRenderer(0, 0);
        renderer.getTableCellRendererComponent(table, "Test", false, true, 0, 0);
        JTextArea textArea = (JTextArea) renderer.getViewport().getView();

        assertEquals(foreground, textArea.getForeground());
        assertEquals(background, textArea.getBackground());
    }

    public void testCellEditing() {
        CsvMultiLineCellRenderer cellRenderer = (CsvMultiLineCellRenderer) fileEditor.getTable().getCellRenderer(0, 0);

        assertTrue(cellRenderer.isCellEditable(null));
        assertTrue(cellRenderer.shouldSelectCell(null));

        final int[] states = {0, 0};
        CellEditorListener listener = new CellEditorListener() {
            @Override
            public void editingStopped(ChangeEvent e) {
                ++states[0];
            }

            @Override
            public void editingCanceled(ChangeEvent e) {
                ++states[1];
            }
        };
        cellRenderer.addCellEditorListener(listener);

        assertTrue(cellRenderer.stopCellEditing());
        assertEquals(1, states[0]);
        cellRenderer.cancelCellEditing();
        assertEquals(1, states[1]);

        cellRenderer.removeCellEditorListener(listener);
        assertTrue(cellRenderer.stopCellEditing());
        assertEquals(1, states[0]);
        cellRenderer.cancelCellEditing();
        assertEquals(1, states[1]);
    }
}
