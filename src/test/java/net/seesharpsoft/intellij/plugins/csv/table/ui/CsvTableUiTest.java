package net.seesharpsoft.intellij.plugins.csv.table.ui;

import com.intellij.openapi.util.UserDataHolderBase;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;

import javax.swing.*;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class CsvTableUiTest extends CsvBasePlatformTestCase {
    public void testCellRendererAcceptsAnOrdinaryKeyListener() {
        KeyAdapter listener = new KeyAdapter() {
        };
        CsvMultiLineCellRenderer renderer = new CsvMultiLineCellRenderer(listener, new UserDataHolderBase());
        JTextArea text = (JTextArea) renderer.getViewport().getView();
        assertTrue(Arrays.asList(text.getKeyListeners()).contains(listener));
    }

    public void testHeaderFlushesTheCommittedCellBeforeStartingColumnInteraction() {
        Model model = new Model();
        CsvTableBase table = new CsvTableBase(model);
        table.setSize(100, 40);
        table.getTableHeader().setSize(100, 30);
        table.setDefaultEditor(Object.class, new DefaultCellEditor(new JTextField()));
        assertTrue(table.editCellAt(0, 0));
        ((JTextField) table.getEditorComponent()).setText("changed");

        table.getTableHeader().dispatchEvent(new MouseEvent(table.getTableHeader(), MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(), MouseEvent.BUTTON1_DOWN_MASK, 10, 10, 1, false, MouseEvent.BUTTON1));

        assertFalse(table.isEditing());
        assertEquals(1, model.flushes);
        assertEquals("changed", model.flushedValue);
    }

    public void testHeaderUsesSnapshotsOnlyWhenTheTableOptsIn() {
        AtomicBoolean snapshots = new AtomicBoolean();
        AtomicInteger paintChecks = new AtomicInteger();
        CsvTableBase table = new CsvTableBase(new Model()) {
            @Override
            protected boolean usesPaintSnapshot() {
                return snapshots.get();
            }

            @Override
            protected boolean deferPaintWithoutReadAccess() {
                paintChecks.incrementAndGet();
                return false;
            }
        };
        table.getTableHeader().setSize(100, 30);
        paint(table.getTableHeader());
        assertEquals(0, paintChecks.get());

        snapshots.set(true);
        paint(table.getTableHeader());
        assertEquals(1, paintChecks.get());
    }

    public void testRowHeaderPaintsTheSuppliedOverlayWithoutAnEditor() {
        CsvTableBase table = new CsvTableBase(new Model());
        new JScrollPane(table);
        JTable header = CsvTableRowUtilities.addRowMarkerColumn(table, 10, row -> Color.RED);
        AtomicInteger paints = new AtomicInteger();
        CsvTableRowUtilities.setRowHeaderOverlay(header, (graphics, target) -> {
            assertSame(header, target);
            paints.incrementAndGet();
        });
        header.setSize(10, 40);

        paint(header);

        assertEquals(1, paints.get());
    }

    private static void paint(JComponent component) {
        BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = image.getGraphics();
        try {
            component.paint(graphics);
        } finally {
            graphics.dispose();
        }
    }

    private static final class Model extends CsvTableModelSwingBase {
        private String value = "original";
        private int flushes;
        private String flushedValue;

        @Override
        public int getRowCount() {
            return 1;
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public String getValue(int row, int column) {
            return value;
        }

        @Override
        public void setValue(String value, int row, int column) {
            this.value = value;
        }

        @Override
        public boolean isCommentRow(int row) {
            return false;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return true;
        }

        @Override
        public void flushPendingUpdates() {
            flushes++;
            flushedValue = value;
        }
    }
}
