package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import net.seesharpsoft.intellij.plugins.csv.editor.table.flatlaf.CsvTableEditorFlatLaf;

public class CsvFlatTableClipboardTest extends CsvTableClipboardTest {
    @Override
    protected CsvTableEditorSwing createEditor() {
        return new CsvTableEditorFlatLaf(getProject(), myFixture.getFile().getVirtualFile()) {
            @Override
            protected CsvTableModelSwing createTableModel() {
                return createTableModelForTest(this);
            }
        };
    }
}
