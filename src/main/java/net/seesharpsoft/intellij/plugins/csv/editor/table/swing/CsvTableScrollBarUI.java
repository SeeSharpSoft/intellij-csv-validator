package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

final class CsvTableScrollBarUI extends BasicScrollBarUI {
    private static final Color THUMB_COLOR = new JBColor(0xB8BCC4, 0x60636B);

    @Override
    protected void installDefaults() {
        super.installDefaults();
        scrollBarWidth = JBUI.scale(12);
        incrGap = 0;
        decrGap = 0;
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return createHiddenButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return createHiddenButton();
    }

    private static JButton createHiddenButton() {
        JButton button = new JButton();
        Dimension size = new Dimension();
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        button.setFocusable(false);
        return button;
    }

    @Override
    protected Dimension getMinimumThumbSize() {
        return scrollbar.getOrientation() == JScrollBar.VERTICAL ? JBUI.size(12, 24) : JBUI.size(24, 12);
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle bounds) {
        g.setColor(UIUtil.getPanelBackground());
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle bounds) {
        if (!c.isEnabled() || bounds.isEmpty()) return;
        int inset = JBUI.scale(2);
        int width = bounds.width - 2 * inset;
        int height = bounds.height - 2 * inset;
        if (width <= 0 || height <= 0) return;
        Graphics2D graphics = (Graphics2D) g.create();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(THUMB_COLOR);
            int arc = Math.min(width, height);
            graphics.fillRoundRect(bounds.x + inset, bounds.y + inset, width, height, arc, arc);
        } finally {
            graphics.dispose();
        }
    }
}
