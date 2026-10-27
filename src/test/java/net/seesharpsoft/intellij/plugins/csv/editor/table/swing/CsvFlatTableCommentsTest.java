package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import net.seesharpsoft.intellij.plugins.csv.editor.table.flatlaf.CsvTableEditorFlatLaf;

import java.awt.*;
import java.awt.image.BufferedImage;

public class CsvFlatTableCommentsTest extends CsvTableCommentsAtBeginningTest {

    @Override
    protected CsvTableEditorSwing createEditor() {
        return new CsvTableEditorFlatLaf(getProject(), myFixture.getFile().getVirtualFile());
    }

    public void testCommentRowSpansAllColumnsAndPaints() {
        CsvTable table = (CsvTable) fileEditor.getTable();
        table.setSize(800, 400);
        assertTrue(table.isCommentRow(0));
        assertEquals(table.getWidth(), table.getCellRect(0, 0, true).width);
        assertTrue(table.getCellRect(0, 1, true).isEmpty());
        assertFalse(table.isCellEditable(0, 1));

        BufferedImage image = new BufferedImage(800, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            table.paint(graphics);
        } finally {
            graphics.dispose();
        }
        assertEquals(0, table.columnAtPoint(new Point(500, table.getRowHeight() / 2)));
    }
}
