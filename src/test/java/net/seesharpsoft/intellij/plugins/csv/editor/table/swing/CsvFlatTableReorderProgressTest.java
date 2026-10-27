package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import net.seesharpsoft.intellij.plugins.csv.editor.table.flatlaf.CsvTableEditorFlatLaf;

public class CsvFlatTableReorderProgressTest extends CsvTableReorderProgressTest {
    @Override
    protected CsvTableEditorSwing createEditor() {
        return new CsvTableEditorFlatLaf(getProject(), myFixture.getFile().getVirtualFile()) {
            @Override
            protected boolean runReorderWithProgress(Runnable operation) {
                return super.runReorderWithProgress(() -> controlledOperation(operation));
            }
        };
    }
}
