package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import net.seesharpsoft.intellij.plugins.csv.editor.table.flatlaf.CsvTableEditorFlatLaf;

public class CsvTableKeymapFlatLafTest extends CsvTableKeymapTest {

    @Override
    protected CsvTableEditorSwing createEditor() {
        return new CsvTableEditorFlatLaf(getProject(), myFixture.getFile().getVirtualFile());
    }
}
