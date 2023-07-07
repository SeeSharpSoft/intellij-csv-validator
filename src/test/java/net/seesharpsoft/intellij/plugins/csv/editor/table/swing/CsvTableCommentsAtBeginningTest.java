package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import javax.swing.table.TableModel;

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
}
