package net.seesharpsoft.intellij.plugins.csv.actions;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.util.IconLoader;
import com.intellij.util.IconUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;

import javax.swing.Icon;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public class CsvTableContextMenuIconsTest extends CsvBasePlatformTestCase {

    public void testRowContextMenuHasScalableLightAndDarkIcons() {
        assertActionGroupIcons("CsvTableEditorRowContextMenu");
    }

    public void testColumnContextMenuHasScalableLightAndDarkIcons() {
        assertActionGroupIcons("CsvTableEditorColumnContextMenu");
    }

    public void testEditorViewSwitchHasScalableLightAndDarkIcons() {
        assertActionGroupIcons(CsvEditorViewActions.GROUP_ID);
    }

    private void assertActionGroupIcons(String groupId) {
        ActionManager manager = ActionManager.getInstance();
        DefaultActionGroup group = (DefaultActionGroup) manager.getAction(groupId);
        for (AnAction action : group.getChildren(manager)) {
            if (action instanceof Separator) continue;
            String name = action.getClass().getSimpleName();
            Icon icon = action.getTemplatePresentation().getIcon();
            assertNotNull(name + " must have an icon", icon);
            assertEquals(name, 16, icon.getIconWidth());
            assertEquals(name, 16, icon.getIconHeight());

            Icon light = IconLoader.getDarkIcon(icon, false);
            Icon dark = IconLoader.getDarkIcon(icon, true);
            int[] lightPixels = renderPixels(light);
            int[] darkPixels = renderPixels(dark);
            assertFalse(name + " must adapt to dark themes", Arrays.equals(lightPixels, darkPixels));

            Icon scaled = IconUtil.scale(icon, null, 2f);
            assertEquals(name, 32, scaled.getIconWidth());
            assertEquals(name, 32, scaled.getIconHeight());
            renderPixels(scaled);
        }
    }

    private int[] renderPixels(Icon icon) {
        BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            icon.paintIcon(null, graphics, 0, 0);
        } finally {
            graphics.dispose();
        }
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        assertTrue("Icon must render visible pixels", Arrays.stream(pixels).anyMatch(pixel -> (pixel >>> 24) != 0));
        return pixels;
    }
}
