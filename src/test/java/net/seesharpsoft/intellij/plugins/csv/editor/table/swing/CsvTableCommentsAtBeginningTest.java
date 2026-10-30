package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.util.ThrowableComputable;

import javax.swing.table.TableModel;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mockStatic;
import org.mockito.MockedStatic;

public class CsvTableCommentsAtBeginningTest extends CsvTableEditorSwingTestBase {
    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/editor/table/swing";
    }

    @Override
    protected String getTestFile() {
        return "CommentsAtBeginning.csv";
    }

    public void testHeaderIsFirstNonCommentLine() {
        TableModel tableModel = fileEditor.getTable().getModel();

        assertEquals("Col1 (1)", tableModel.getColumnName(0));
        assertEquals("Col2 (2)", tableModel.getColumnName(1));
        assertEquals("Col3 (3)", tableModel.getColumnName(2));
    }

    public void testGetCellRectDoesNotReadPsi() {
        CsvTable table = (CsvTable) fileEditor.getTable();
        assertTrue(table.isCommentRow(0));

        try (MockedStatic<ReadAction> readAction = mockStatic(ReadAction.class)) {
            table.getCellRect(0, 0, true);

            readAction.verify(() -> ReadAction.compute(any(ThrowableComputable.class)), never());
        }
    }

    public void testCommentRowSpansAllColumnsAndPaints() {
        CsvTable table = fileEditor.getTable();
        table.setSize(800, 400);
        assertTrue(table.isCommentRow(0));
        assertEquals(table.getWidth(), table.getCellRect(0, 0, true).width);
        assertEquals(table.getCellRect(0, 0, true), table.getCellRect(0, 1, true));
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
