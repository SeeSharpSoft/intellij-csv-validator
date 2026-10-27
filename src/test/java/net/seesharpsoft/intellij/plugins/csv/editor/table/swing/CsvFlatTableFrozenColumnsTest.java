package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import net.seesharpsoft.intellij.plugins.csv.editor.table.flatlaf.CsvTableEditorFlatLaf;

public class CsvFlatTableFrozenColumnsTest extends CsvTableFrozenColumnsTest {
    @Override
    protected CsvTableEditorSwing createEditor() {
        return new CsvTableEditorFlatLaf(getProject(), myFixture.getFile().getVirtualFile());
    }
}
