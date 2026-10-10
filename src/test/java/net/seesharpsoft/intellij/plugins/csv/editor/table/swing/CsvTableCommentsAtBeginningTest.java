package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.util.ThrowableComputable;

import javax.swing.table.TableModel;

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
}
