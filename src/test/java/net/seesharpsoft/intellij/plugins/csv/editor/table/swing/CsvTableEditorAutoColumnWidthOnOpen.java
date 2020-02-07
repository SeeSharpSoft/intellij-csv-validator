package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

public class CsvTableEditorAutoColumnWidthOnOpen extends CsvTableEditorSwingTestBase {

    @Override
    protected void initializeEditorSettings(CsvEditorSettings instance) {
        super.initializeEditorSettings(instance);
        instance.setTableAutoColumnWidthOnOpen(true);
    }

    public void testAutoColumnWidthOnFirstOpen() {
        assertTrue("first column is bigger than second", fileEditor.getTable().getColumnModel().getColumn(0).getWidth() > fileEditor.getTable().getColumnModel().getColumn(1).getWidth());
    }
}
