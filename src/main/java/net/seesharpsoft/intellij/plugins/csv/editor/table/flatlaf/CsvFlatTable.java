package net.seesharpsoft.intellij.plugins.csv.editor.table.flatlaf;

import com.formdev.flatlaf.ui.FlatTableHeaderUI;
import com.formdev.flatlaf.ui.FlatTableUI;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import net.seesharpsoft.intellij.plugins.csv.editor.table.swing.CsvTable;
import net.seesharpsoft.intellij.plugins.csv.editor.table.swing.CsvTableModelSwing;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.plaf.TableUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

final class CsvFlatTable extends CsvTable {

    CsvFlatTable(CsvTableModelSwing model) {
        super(model);
    }

    @Override
    protected TableUI createTableUI() {
        return new FlatSpanningTableUI();
    }

    @Override
    protected @NotNull JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {
            @Override
            public void updateUI() {
                setUI(new FlatTableHeaderUI());
                setBackground(UIUtil.getPanelBackground());
                setForeground(UIUtil.getLabelForeground());
                setFont(UIUtil.getLabelFont().deriveFont(Font.BOLD));
                setDefaultRenderer(new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                                    boolean focused, int row, int column) {
                        super.getTableCellRendererComponent(table, value, false, false, row, column);
                        JTableHeader header = table.getTableHeader();
                        setBackground(header.getBackground());
                        setForeground(header.getForeground());
                        setFont(header.getFont());
                        setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createMatteBorder(0, 0, 1, 1, JBColor.border()),
                                JBUI.Borders.empty(8, 10)));
                        return this;
                    }
                });
            }
        };
    }

    private class FlatSpanningTableUI extends FlatTableUI {
        @Override
        protected void installDefaults() {
            super.installDefaults();
            // Install FlatLaf on this table only; the IDE retains its own look and feel and defaults.
            EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
            table.setBackground(scheme.getDefaultBackground());
            table.setForeground(scheme.getDefaultForeground());
            selectionBackground = UIUtil.getTableSelectionBackground(true);
            selectionForeground = UIUtil.getTableSelectionForeground(true);
            selectionInactiveBackground = UIUtil.getTableSelectionBackground(false);
            selectionInactiveForeground = UIUtil.getTableSelectionForeground(false);
            table.setSelectionBackground(selectionBackground);
            table.setSelectionForeground(selectionForeground);
            table.setGridColor(JBColor.border());
            table.setShowHorizontalLines(true);
            table.setShowVerticalLines(false);
            table.setIntercellSpacing(JBUI.size(0, 1));
            cellMargins = JBUI.insets(3, 8);
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            paintTable(g, rendererPane, () -> super.paint(g, c));
        }
    }
}
