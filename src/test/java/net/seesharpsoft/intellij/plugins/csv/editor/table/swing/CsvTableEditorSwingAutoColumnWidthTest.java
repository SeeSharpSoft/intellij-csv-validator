package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;

public class CsvTableEditorSwingAutoColumnWidthTest extends CsvTableEditorSwingTestBase {

    @Override
    protected CsvTableEditorSwing createEditor() {
        myFixture.configureByText("AutoFit.csv", "Description,Value\nx,0123456789012345678901234567890123456789\n");
        return new CsvTableEditorSwing(getProject(), myFixture.getFile().getVirtualFile());
    }

    @Override
    protected void initializeEditorSettings(CsvEditorSettings settings) {
        super.initializeEditorSettings(settings);
        settings.setTableAutoMaxColumnWidth(0);
    }

    public void testAutoFitIncludesHeaderTextAndPadding() {
        assertAutoFitIncludesHeaderTextAndPadding();
    }

    public void testAutoFitIncludesHeaderAfterZoomingOut() {
        fileEditor.changeFontSize(-3);
        assertAutoFitIncludesHeaderTextAndPadding();
    }

    private void assertAutoFitIncludesHeaderTextAndPadding() {
        fileEditor.tableEditorActions.adjustColumnWidthAction.actionPerformed(null);

        JTable table = fileEditor.getTable();
        TableColumn column = table.getColumnModel().getColumn(0);
        Component header = table.getTableHeader().getDefaultRenderer().getTableCellRendererComponent(
                table, column.getHeaderValue(), false, false, -1, 0);
        assertTrue("Auto-fit width " + column.getWidth() + " must fit header width " + header.getPreferredSize().width,
                column.getWidth() >= header.getPreferredSize().width);
    }

    public void testAutoFitIncludesCellPadding() {
        assertAutoFitIncludesCellPadding();
    }

    public void testAutoFitIncludesCellPaddingAfterZoomingIn() {
        fileEditor.changeFontSize(8);
        assertAutoFitIncludesCellPadding();
    }

    public void testAutoFitIncludesCellPaddingAfterZoomingOut() {
        fileEditor.changeFontSize(-3);
        assertAutoFitIncludesCellPadding();
    }

    public void testAutoFitRespectsMaximumColumnWidth() {
        CsvEditorSettings.getInstance().setTableAutoMaxColumnWidth(60);
        fileEditor.tableEditorActions.adjustColumnWidthAction.actionPerformed(null);

        for (int column = 0; column < fileEditor.getTable().getColumnCount(); column++) {
            assertEquals(60, fileEditor.getTable().getColumnModel().getColumn(column).getWidth());
        }
    }

    public void testAutoFitKeepsMultilineRenderingEnabled() {
        fileEditor.tableEditorActions.adjustColumnWidthAction.actionPerformed(null);

        JTable table = fileEditor.getTable();
        JScrollPane cell = (JScrollPane) table.prepareRenderer(table.getCellRenderer(1, 1), 1, 1);
        assertTrue(((JTextArea) cell.getViewport().getView()).getLineWrap());
    }

    private void assertAutoFitIncludesCellPadding() {
        fileEditor.tableEditorActions.adjustColumnWidthAction.actionPerformed(null);

        JTable table = fileEditor.getTable();
        JScrollPane cell = (JScrollPane) table.prepareRenderer(table.getCellRenderer(1, 1), 1, 1);
        JTextArea textArea = (JTextArea) cell.getViewport().getView();
        textArea.setLineWrap(false);
        int requiredWidth = textArea.getPreferredSize().width + cell.getInsets().left + cell.getInsets().right
                + table.getIntercellSpacing().width;
        int actualWidth = table.getColumnModel().getColumn(1).getWidth();
        assertTrue("Auto-fit width " + actualWidth + " must fit cell width " + requiredWidth,
                actualWidth >= requiredWidth);
    }
}
