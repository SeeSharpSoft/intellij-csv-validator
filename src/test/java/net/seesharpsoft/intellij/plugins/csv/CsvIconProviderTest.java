package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IconUtil;
import net.seesharpsoft.intellij.plugins.csv.components.CsvValueSeparator;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvField;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvColorSettings;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.plugins.psv.PsvFileType;
import net.seesharpsoft.intellij.plugins.tsv.TsvFileType;
import net.seesharpsoft.intellij.psi.PsiHelper;

import javax.swing.Icon;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CsvIconProviderTest extends CsvBasePlatformTestCase {

    public void testFileTypesAndPsiFilesUseThemedScalableIcons() {
        CsvIconProvider provider = new CsvIconProvider();
        for (LanguageFileType type : new LanguageFileType[]{CsvFileType.INSTANCE, TsvFileType.INSTANCE, PsvFileType.INSTANCE}) {
            PsiFile file = myFixture.configureByText(type, "first,second");
            Icon icon = type.getIcon();
            assertSame(icon, provider.getIcon(file, 0));
            assertThemedScalableIcon(type.getName(), icon);
        }
        assertSame(CsvIconProvider.FILE, new CsvColorSettings().getIcon());
    }

    public void testFileTypesHaveDistinctAccentColorsInBothThemes() {
        for (boolean dark : new boolean[]{false, true}) {
            int csv = dominantColor(CsvFileType.INSTANCE, dark);
            int tsv = dominantColor(TsvFileType.INSTANCE, dark);
            int psv = dominantColor(PsvFileType.INSTANCE, dark);
            assertTrue("CSV and TSV must have distinct accent colors", csv != tsv);
            assertTrue("CSV and PSV must have distinct accent colors", csv != psv);
            assertTrue("TSV and PSV must have distinct accent colors", tsv != psv);
        }
    }

    public void testPsiFieldsUseThemedScalableIcons() {
        PsiFile file = myFixture.configureByText(CsvFileType.INSTANCE, "first,second");
        CsvField field = PsiTreeUtil.findChildOfType(file, CsvField.class);
        assertNotNull(field);
        assertSame(CsvIconProvider.FIELD, new CsvIconProvider().getIcon(field, 0));
        assertThemedScalableIcon("Field", CsvIconProvider.FIELD);
    }

    public void testValueSeparatorsUseThemedScalableChangeSeparatorIcon() {
        assertValueSeparatorIcon(CsvFileType.INSTANCE, ",");
        assertValueSeparatorIcon(TsvFileType.INSTANCE, "\t");
        assertValueSeparatorIcon(PsvFileType.INSTANCE, "|");
        assertThemedScalableIcon("Separator",
                IconLoader.getIcon("/media/icons/actions/change-separator.svg", CsvIconProvider.class));
    }

    public void testCustomMultiCharacterValueSeparatorUsesChangeSeparatorIcon() {
        CsvEditorSettings settings = CsvEditorSettings.getInstance();
        CsvValueSeparator previousSeparator = settings.getDefaultValueSeparator();
        boolean previousAutoDetect = settings.isAutoDetectValueSeparator();
        try {
            settings.setDefaultValueSeparator(new CsvValueSeparator("||"));
            settings.setAutoDetectValueSeparator(false);
            assertValueSeparatorIcon(CsvFileType.INSTANCE, "||");
        } finally {
            settings.setDefaultValueSeparator(previousSeparator);
            settings.setAutoDetectValueSeparator(previousAutoDetect);
        }
    }

    private void assertValueSeparatorIcon(LanguageFileType type, String separator) {
        String quotedField = "\"first" + separator + "value\"";
        PsiFile file = myFixture.configureByText(
                getTestName(true) + "." + type.getDefaultExtension(), quotedField + separator + "second");
        assertSame(type, file.getFileType());
        PsiElement separatorElement = file.findElementAt(quotedField.length());
        assertNotNull(separatorElement);
        assertSame(CsvTypes.COMMA, PsiHelper.getElementType(separatorElement));
        assertEquals(separator, separatorElement.getText());
        CsvIconProvider provider = new CsvIconProvider();
        assertSame(type.getName(),
                IconLoader.getIcon("/media/icons/actions/change-separator.svg", CsvIconProvider.class),
                provider.getIcon(separatorElement, 0));

        PsiElement quotedSeparator = file.findElementAt("\"first".length());
        assertNotNull(quotedSeparator);
        assertNull(provider.getIcon(quotedSeparator, 0));
    }

    private void assertThemedScalableIcon(String name, Icon icon) {
        assertNotNull(name, icon);
        int[] light = renderPixels(IconLoader.getDarkIcon(icon, false));
        int[] dark = renderPixels(IconLoader.getDarkIcon(icon, true));
        assertFalse(name + " must adapt to dark themes", Arrays.equals(light, dark));
        for (boolean darkTheme : new boolean[]{false, true}) {
            for (float scale : new float[]{1f, 2f}) {
                Icon scaled = IconUtil.scale(IconLoader.getDarkIcon(icon, darkTheme), null, scale);
                assertEquals(name, (int) (16 * scale), scaled.getIconWidth());
                assertEquals(name, (int) (16 * scale), scaled.getIconHeight());
                assertTrue(name + " must render visible pixels",
                        Arrays.stream(renderPixels(scaled)).anyMatch(pixel -> (pixel >>> 24) != 0));
            }
        }
    }

    private int dominantColor(LanguageFileType type, boolean dark) {
        Icon icon = type.getIcon();
        assertNotNull(type.getName(), icon);
        Map<Integer, Integer> counts = new HashMap<>();
        for (int pixel : renderPixels(IconLoader.getDarkIcon(icon, dark))) {
            if ((pixel >>> 24) == 255) {
                counts.merge(pixel & 0xFFFFFF, 1, Integer::sum);
            }
        }
        return Collections.max(counts.entrySet(), Map.Entry.comparingByValue()).getKey();
    }

    private int[] renderPixels(Icon icon) {
        BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            icon.paintIcon(null, graphics, 0, 0);
        } finally {
            graphics.dispose();
        }
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }
}
