package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.formdev.flatlaf.ui.FlatScrollBarUI;
import com.formdev.flatlaf.ui.FlatTableHeaderUI;
import com.formdev.flatlaf.ui.FlatTableUI;
import net.seesharpsoft.intellij.plugins.csv.editor.table.flatlaf.CsvTableEditorFlatLaf;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class CsvTableEditorFlatLafTest extends CsvTableEditorSwingTest {

    @Override
    protected CsvTableEditorSwing createEditor() {
        return new CsvTableEditorFlatLaf(getProject(), myFixture.getFile().getVirtualFile());
    }

    public void testFlatLafDelegatesSurviveThemeRefresh() {
        JTable table = fileEditor.getTable();
        JScrollPane scrollPane = fileEditor.getTableScrollPane();
        LookAndFeel ideLookAndFeel = UIManager.getLookAndFeel();
        Object tableDefaults = UIManager.get("Table.background");

        SwingUtilities.updateComponentTreeUI(fileEditor.getComponent());

        assertInstanceOf(table.getUI(), FlatTableUI.class);
        assertInstanceOf(table.getTableHeader().getUI(), FlatTableHeaderUI.class);
        assertInstanceOf(scrollPane.getVerticalScrollBar().getUI(), FlatScrollBarUI.class);
        assertInstanceOf(scrollPane.getHorizontalScrollBar().getUI(), FlatScrollBarUI.class);
        assertSame(ideLookAndFeel, UIManager.getLookAndFeel());
        assertSame(tableDefaults, UIManager.get("Table.background"));
    }

    public void testMultilineCellEditUpdatesCsv() {
        JTable table = fileEditor.getTable();
        assertTrue(table.editCellAt(1, 1));
        JScrollPane cell = (JScrollPane) table.getEditorComponent();
        JTextArea text = (JTextArea) cell.getViewport().getView();
        text.setText("first line\nsecond line");
        assertTrue(table.getCellEditor().stopCellEditing());

        assertEquals("first line\nsecond line", fileEditor.getTableModel().getValue(1, 1));
        assertTrue(myFixture.getFile().getText().contains("\"first line\nsecond line\""));
    }

    public void testFlatTableAndHeaderPaint() {
        JTable table = fileEditor.getTable();
        table.setSize(800, 400);
        table.getTableHeader().setSize(800, 40);
        BufferedImage image = new BufferedImage(800, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            table.paint(graphics);
            table.getTableHeader().paint(graphics);
        } finally {
            graphics.dispose();
        }
    }
}
