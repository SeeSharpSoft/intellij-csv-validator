package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import net.seesharpsoft.intellij.plugins.csv.editor.table.flatlaf.CsvTableEditorFlatLaf;

public class CsvTableFlatLafMovementTest extends CsvTableMovementTest {
    @Override
    protected CsvTableEditorSwing createEditor() {
        if (updateScheduler == null) {
            return new CsvTableEditorFlatLaf(getProject(), myFixture.getFile().getVirtualFile());
        }
        return new CsvTableEditorFlatLaf(getProject(), myFixture.getFile().getVirtualFile()) {
            @Override
            protected CsvTableModelSwing createTableModel() {
                return createTableModelForTest(this);
            }
        };
    }
}
