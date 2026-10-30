package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

final class CsvTablePaintSnapshot {
    private BufferedImage image;
    private Rectangle bounds;
    private double scale;

    void paint(JComponent component, Graphics graphics, boolean readAccess, Consumer<Graphics> painter) {
        Rectangle visible = component.getVisibleRect();
        if (readAccess) {
            if (visible.isEmpty()) {
                painter.accept(graphics);
                return;
            }
            if (!update(graphics, visible, painter)) return;
        } else if (component.isOpaque()) {
            Graphics background = graphics.create();
            try {
                background.setColor(component.getBackground());
                background.fillRect(0, 0, component.getWidth(), component.getHeight());
            } finally {
                background.dispose();
            }
        }
        if (image != null) {
            // Keep a complete previous frame even when scrolling changed the viewport origin.
            UIUtil.drawImage(graphics, image, visible.x, visible.y, null);
        }
    }

    private boolean update(Graphics graphics, Rectangle visible, Consumer<Graphics> painter) {
        double currentScale = graphics instanceof Graphics2D graphics2D ? JBUIScale.sysScale(graphics2D) : 1;
        boolean resized = image == null || bounds.width != visible.width || bounds.height != visible.height ||
                Double.compare(scale, currentScale) != 0;
        Shape clip = graphics.getClip();
        if ((resized || !visible.equals(bounds)) && clip != null && !clip.contains(visible)) {
            // Row-drag previews and partial repaints must keep their original clipping.
            painter.accept(graphics);
            return false;
        }
        if (resized) {
            clear();
            image = ImageUtil.createImage(graphics, visible.width, visible.height, BufferedImage.TYPE_INT_ARGB);
        }
        Graphics2D buffer = image.createGraphics();
        boolean painted = false;
        try {
            if (graphics instanceof Graphics2D graphics2D) {
                buffer.addRenderingHints(graphics2D.getRenderingHints());
            }
            buffer.setFont(graphics.getFont());
            buffer.setColor(graphics.getColor());
            buffer.translate(-visible.x, -visible.y);
            buffer.setClip(visible);
            if (clip != null) buffer.clip(clip);
            buffer.setComposite(AlphaComposite.Clear);
            buffer.fillRect(visible.x, visible.y, visible.width, visible.height);
            buffer.setComposite(AlphaComposite.SrcOver);
            painter.accept(buffer);
            painted = true;
        } finally {
            buffer.dispose();
            if (!painted) clear();
        }
        bounds = visible;
        scale = currentScale;
        return true;
    }

    void clear() {
        if (image != null) image.flush();
        image = null;
        bounds = null;
    }
}
