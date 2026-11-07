package net.seesharpsoft.intellij.plugins.csv.diff;

import com.intellij.openapi.editor.colors.ColorKey;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.ui.Gray;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.JBUI;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Paints the wavy line of the text diff's collapsed unchanged fragments with the same color scheme keys and shape.
 */
final class CsvDiffFoldWave {
    private static final ColorKey WAVE_COLOR = ColorKey.find("DIFF_SEPARATOR_WAVE");
    private static final ColorKey LEGACY_WAVE_COLOR = ColorKey.find("DIFF_SEPARATORS_BACKGROUND");
    private static final int STEP = 5;
    private static final int AMPLITUDE = 3;

    private CsvDiffFoldWave() {
    }

    /**
     * Returns the x coordinate of the component in its window. Waves of the tables and the divider share this origin,
     * so they join into one continuous line that keeps its position while the tables scroll horizontally.
     */
    static int originX(Component component) {
        int x = 0;
        for (Component current = component; current != null && !(current instanceof Window); current = current.getParent()) {
            x += current.getX();
        }
        return x;
    }

    /**
     * Paints the wave between {@code x1} and {@code x2}, vertically centered in the given row.
     *
     * @param originX the x coordinate of the graphics' origin in the window, see {@link #originX(Component)}
     */
    static void paint(Graphics g, int x1, int x2, int y, int height, int originX) {
        if (x2 <= x1) return;
        int step = JBUI.scale(STEP);
        int amplitude = JBUI.scale(AMPLITUDE);
        int period = 4 * step;
        double top = y + height / 2.0 - amplitude;
        double bottom = top + 2 * amplitude;
        double center = top + amplitude;
        double start = x1 - (double) Math.floorMod(x1 + originX, period);
        Path2D path = new Path2D.Double();
        path.moveTo(start, top);
        for (double x = start; x < x2; x += period) {
            path.quadTo(x + step * 0.5, top, x + step, center);
            path.quadTo(x + step * 1.5, bottom, x + step * 2, bottom);
            path.quadTo(x + step * 2.5, bottom, x + step * 3, center);
            path.quadTo(x + step * 3.5, top, x + step * 4, top);
        }
        Graphics2D wave = (Graphics2D) g.create();
        try {
            wave.clipRect(x1, y, x2 - x1, height);
            GraphicsUtil.setupAAPainting(wave);
            wave.setColor(color());
            wave.setStroke(new BasicStroke(1f));
            wave.draw(path);
        } finally {
            wave.dispose();
        }
    }

    private static Color color() {
        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
        Color color = scheme.getColor(WAVE_COLOR);
        if (color == null) color = scheme.getColor(LEGACY_WAVE_COLOR);
        return color == null ? Gray._128 : color;
    }
}
