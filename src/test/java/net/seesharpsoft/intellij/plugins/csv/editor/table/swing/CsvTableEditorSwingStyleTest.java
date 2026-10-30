package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.image.BufferedImage;

public class CsvTableEditorSwingStyleTest extends CsvTableEditorSwingTestBase {

    public void testSwingUsesFlatStylingWithoutReplacingIdeLookAndFeel() {
        LookAndFeel lookAndFeel = UIManager.getLookAndFeel();
        Object tableDefaults = UIManager.get("Table.background");
        assertStyledComponents();

        SwingUtilities.updateComponentTreeUI(fileEditor.getComponent());

        assertStyledComponents();
        assertInstanceOf(fileEditor.getTable().getUI(), CsvTable.MultiSpanCellTableUI.class);
        assertSame(lookAndFeel, UIManager.getLookAndFeel());
        assertSame(tableDefaults, UIManager.get("Table.background"));
    }

    private void assertStyledComponents() {
        JTable table = fileEditor.getTable();
        JScrollPane scrollPane = fileEditor.getTableScrollPane();
        assertInstanceOf(scrollPane.getVerticalScrollBar().getUI(), CsvTableScrollBarUI.class);
        assertInstanceOf(scrollPane.getHorizontalScrollBar().getUI(), CsvTableScrollBarUI.class);
        assertEquals(JBUI.insets(8), fileEditor.getComponent().getInsets());
        assertEquals(JBUI.insets(1), scrollPane.getInsets());
        assertTrue(table.getShowHorizontalLines());
        assertFalse(table.getShowVerticalLines());
        assertEquals(new Dimension(0, JBUI.scale(1)), table.getIntercellSpacing());
        assertEquals(JBColor.border(), table.getGridColor());
        assertEquals(table.getBackground(), scrollPane.getViewport().getBackground());

        JTableHeader header = table.getTableHeader();
        JComponent cell = (JComponent) header.getDefaultRenderer().getTableCellRendererComponent(
                table, "Header", false, false, -1, 0);
        assertEquals(UIUtil.getPanelBackground(), cell.getBackground());
        assertEquals(UIUtil.getLabelForeground(), cell.getForeground());
        assertTrue(cell.getFont().isBold());
        Insets padding = JBUI.insets(8, 10);
        assertEquals(new Insets(padding.top, padding.left, padding.bottom + 1, padding.right + 1), cell.getInsets());

        JTable rowHeader = (JTable) scrollPane.getRowHeader().getView();
        assertEquals(header.getBackground(), rowHeader.getBackground());
        assertEquals(header.getForeground(), rowHeader.getForeground());
        assertFalse(rowHeader.getShowVerticalLines());
        assertInstanceOf(scrollPane.getCorner(JScrollPane.UPPER_LEFT_CORNER), CsvTableHeader.class);
    }

    public void testCellPaddingDoesNotJumpWhenFocusedOrEditing() {
        JTable table = fileEditor.getTable();
        CsvMultiLineCellRenderer renderer = (CsvMultiLineCellRenderer) table.getDefaultRenderer(Object.class);
        for (boolean focused : new boolean[]{false, true}) {
            renderer.getTableCellRendererComponent(table, "Value", true, focused, 1, 0);
            JTextArea text = (JTextArea) renderer.getViewport().getView();
            assertEquals(JBUI.insets(3, 8), text.getInsets());
            assertEquals(table.getSelectionBackground(), text.getBackground());
            assertEquals(table.getSelectionForeground(), text.getForeground());
        }

        assertTrue(table.editCellAt(1, 0));
        JScrollPane editor = (JScrollPane) table.getEditorComponent();
        assertEquals(JBUI.insets(3, 8), ((JTextArea) editor.getViewport().getView()).getInsets());
        table.getCellEditor().cancelCellEditing();
    }

    public void testColorSchemeRefreshUpdatesTableAndEmptyViewport() {
        JTable table = fileEditor.getTable();
        table.setBackground(Color.ORANGE);
        table.setForeground(Color.MAGENTA);
        EditorColorsManager manager = EditorColorsManager.getInstance();

        ApplicationManager.getApplication().getMessageBus().syncPublisher(EditorColorsManager.TOPIC)
                .globalSchemeChange(manager.getGlobalScheme());

        assertEquals(manager.getGlobalScheme().getDefaultBackground(), table.getBackground());
        assertEquals(manager.getGlobalScheme().getDefaultForeground(), table.getForeground());
        assertEquals(table.getBackground(), fileEditor.getTableScrollPane().getViewport().getBackground());
    }

    public void testScrollbarsHaveRoundedInsetThumbsAndNoArrowButtons() {
        JScrollPane scrollPane = fileEditor.getTableScrollPane();
        for (JScrollBar scrollBar : new JScrollBar[]{scrollPane.getVerticalScrollBar(), scrollPane.getHorizontalScrollBar()}) {
            boolean vertical = scrollBar.getOrientation() == JScrollBar.VERTICAL;
            int thickness = JBUI.scale(12);
            assertEquals(thickness, vertical ? scrollBar.getPreferredSize().width : scrollBar.getPreferredSize().height);
            scrollBar.setSize(vertical ? new Dimension(thickness, 200) : new Dimension(200, thickness));
            scrollBar.setValues(20, 30, 0, 100);
            scrollBar.doLayout();
            for (Component button : scrollBar.getComponents()) {
                assertTrue(button.getBounds().isEmpty());
            }

            BufferedImage image = new BufferedImage(scrollBar.getWidth(), scrollBar.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            try {
                scrollBar.paint(graphics);
            } finally {
                graphics.dispose();
            }
            int thumbColor = new JBColor(0xB8BCC4, 0x60636B).getRGB();
            int firstThumbWidth = 0;
            int maxThumbWidth = 0;
            for (int position = 0; position < 200; position++) {
                int coloredPixels = 0;
                for (int cross = 0; cross < thickness; cross++) {
                    if (image.getRGB(vertical ? cross : position, vertical ? position : cross) == thumbColor) coloredPixels++;
                }
                if (firstThumbWidth == 0 && coloredPixels > 0) firstThumbWidth = coloredPixels;
                maxThumbWidth = Math.max(maxThumbWidth, coloredPixels);
            }
            assertTrue("The scrollbar must paint a visible thumb", firstThumbWidth > 0);
            assertTrue("The thumb must have rounded ends", maxThumbWidth > firstThumbWidth);
            assertEquals(thickness - 2 * JBUI.scale(2), maxThumbWidth);
        }
    }
}
