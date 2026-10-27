package net.seesharpsoft.intellij.plugins.csv.editor.table.flatlaf;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.ui.FlatScrollBarUI;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.colors.EditorColorsListener;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import net.seesharpsoft.intellij.plugins.csv.editor.table.swing.CsvMultiLineCellRenderer;
import net.seesharpsoft.intellij.plugins.csv.editor.table.swing.CsvTable;
import net.seesharpsoft.intellij.plugins.csv.editor.table.swing.CsvTableEditorSwing;
import net.seesharpsoft.intellij.plugins.csv.editor.table.swing.CsvTableModelSwing;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.Map;

public class CsvTableEditorFlatLaf extends CsvTableEditorSwing {

    public CsvTableEditorFlatLaf(@NotNull Project project, @NotNull VirtualFile file) {
        super(project, file);
        getComponent().setBorder(JBUI.Borders.empty(8));
        JScrollPane scrollPane = getTableScrollPane();
        scrollPane.setBorder(JBUI.Borders.customLine(JBColor.border()));
        scrollPane.getViewport().setBackground(getTable().getBackground());
        styleScrollBar(scrollPane.getVerticalScrollBar());
        styleScrollBar(scrollPane.getHorizontalScrollBar());
        ApplicationManager.getApplication().getMessageBus().connect(this)
                .subscribe(EditorColorsManager.TOPIC, (EditorColorsListener) scheme -> {
                    getTable().updateUI();
                    scrollPane.getViewport().setBackground(getTable().getBackground());
                });
    }

    @Override
    protected CsvTable createTable(CsvTableModelSwing model) {
        return new CsvFlatTable(model);
    }

    @Override
    protected CsvMultiLineCellRenderer createCellRenderer(boolean comment) {
        return new CsvFlatCellRenderer(tableEditorKeyListener, this, comment);
    }

    @Override
    protected int[] getMaxTextWidthForAllColumns() {
        JTable table = getTable();
        int[] widths = new int[table.getColumnCount()];
        int sampleSize = Math.min(table.getRowCount(), COLUMN_WIDTH_MAX_TEXT_SAMPLE_SIZE);
        float zoomFactor = getZoomFactor();
        for (int column = 0; column < widths.length; column++) {
            TableColumn tableColumn = table.getColumnModel().getColumn(column);
            TableCellRenderer headerRenderer = tableColumn.getHeaderRenderer();
            if (headerRenderer == null) {
                headerRenderer = table.getTableHeader().getDefaultRenderer();
            }
            int width = headerRenderer.getTableCellRendererComponent(table, tableColumn.getHeaderValue(),
                    false, false, -1, column).getPreferredSize().width;
            for (int row = 0; row < sampleSize; row++) {
                if (getTableModel().isCommentRow(row)) continue;
                CsvFlatCellRenderer renderer = (CsvFlatCellRenderer) table.prepareRenderer(table.getCellRenderer(row, column), row, column);
                width = Math.max(width, renderer.getPreferredUnwrappedWidth() + table.getIntercellSpacing().width);
            }
            // Layout applies zoom to stored widths; renderer measurements are already in screen pixels.
            widths[column] = (int) Math.ceil(width / zoomFactor);
        }
        return widths;
    }

    private static void styleScrollBar(JScrollBar scrollBar) {
        applyScrollBarStyle(scrollBar);
        scrollBar.addPropertyChangeListener("UI", event -> {
            if (!(scrollBar.getUI() instanceof FlatScrollBarUI)) {
                applyScrollBarStyle(scrollBar);
            }
        });
    }

    private static void applyScrollBarStyle(JScrollBar scrollBar) {
        scrollBar.putClientProperty(FlatClientProperties.STYLE, Map.of(
                "width", 12,
                "thumbArc", 999,
                "thumbInsets", JBUI.insets(2), // FlatLaf scales these logical units itself.
                "thumb", new JBColor(0xB8BCC4, 0x60636B),
                "track", UIUtil.getPanelBackground(),
                "showButtons", false));
        scrollBar.setUI(new FlatScrollBarUI());
    }
}
