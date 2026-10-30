package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.ui.table.JBTable;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.mockito.Mockito.mock;

public class CsvTableSelectionPerformanceTest extends CsvBasePlatformTestCase {
    public void testRestoringOneHundredThousandSelectedRowsEmitsBoundedEvents() {
        int count = 100_000;
        JTable table = new JBTable(new DefaultTableModel(count, 4));
        new JScrollPane(table);
        JTable headers = CsvTableRowUtilities.addNumberColumn(mock(CsvTableEditor.class), table, 1);
        AtomicInteger tableEvents = new AtomicInteger();
        AtomicInteger headerEvents = new AtomicInteger();
        table.getSelectionModel().addListSelectionListener(event -> tableEvents.incrementAndGet());
        headers.getSelectionModel().addListSelectionListener(event -> headerEvents.incrementAndGet());
        int[] rows = IntStream.range(0, count).toArray();
        long start = System.nanoTime();
        CsvTableRowUtilities.setSelection(table.getSelectionModel(), rows, count);
        CsvTableRowUtilities.setSelection(table.getSelectionModel(), rows, count);
        long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        assertTrue("Table selection events: " + tableEvents, tableEvents.get() <= 8);
        assertTrue("Row-header selection events: " + headerEvents, headerEvents.get() <= 8);
        assertEquals(count, table.getSelectedRowCount());
        assertEquals(count, headers.getSelectedRowCount());
        assertTrue("Selection restoration took " + millis + " ms", millis < 1500);
        System.out.println("100,000-row selection restoration: " + millis + " ms, " +
                tableEvents + " table events, " + headerEvents + " row-header events");
    }

    public void testGrowingRowNumbersEmitsOneModelEvent() {
        DefaultTableModel model = new DefaultTableModel(1, 4);
        JTable table = new JBTable(model);
        new JScrollPane(table);
        JTable headers = CsvTableRowUtilities.addNumberColumn(mock(CsvTableEditor.class), table, 1);
        AtomicInteger events = new AtomicInteger();
        headers.getModel().addTableModelListener(event -> events.incrementAndGet());
        model.setRowCount(100_000);
        assertEquals(1, events.get());
        assertEquals(100_000, headers.getRowCount());
        assertEquals(100_000, headers.getValueAt(99_999, 0));
        model.setRowCount(2);
        assertEquals(2, events.get());
        assertEquals(2, headers.getRowCount());
    }
}
