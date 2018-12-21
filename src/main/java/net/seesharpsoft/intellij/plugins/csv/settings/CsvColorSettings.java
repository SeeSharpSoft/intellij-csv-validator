package net.seesharpsoft.intellij.plugins.csv.settings;

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import net.seesharpsoft.intellij.plugins.csv.CsvIconProvider;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import net.seesharpsoft.intellij.plugins.csv.highlighter.CsvSyntaxHighlighter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public class CsvColorSettings implements ColorSettingsPage {

    private static final Integer MAX_COLUMN_HIGHLIGHT_COLORS = 10;
    private static final AttributesDescriptor[] DESCRIPTORS;
    private static final List<TextAttributesKey> COLUMN_HIGHLIGHT_ATTRIBUTES;
    private static final Key<List<TextAttributes>> COLUMN_HIGHLIGHT_TEXT_ATTRIBUTES_KEY = Key.create("CSV_PLUGIN_COLUMN_HIGHLIGHT_ATTRIBUTES");

    static {
        List<AttributesDescriptor> attributesDescriptors = new ArrayList();
        attributesDescriptors.add(new AttributesDescriptor("Separator", CsvSyntaxHighlighter.COMMA));
        attributesDescriptors.add(new AttributesDescriptor("Quote", CsvSyntaxHighlighter.QUOTE));
        attributesDescriptors.add(new AttributesDescriptor("Text", CsvSyntaxHighlighter.TEXT));
        attributesDescriptors.add(new AttributesDescriptor("Escaped Text", CsvSyntaxHighlighter.ESCAPED_TEXT));

        COLUMN_HIGHLIGHT_ATTRIBUTES = new ArrayList<>();
        for (int i = 0; i < MAX_COLUMN_HIGHLIGHT_COLORS; ++i) {
            TextAttributesKey textAttributesKey = createTextAttributesKey(String.format("CSV_COLUMN_HIGHLIGHT_ATTRIBUTE_%d", i + 1), DefaultLanguageHighlighterColors.STRING);
            COLUMN_HIGHLIGHT_ATTRIBUTES.add(textAttributesKey);
            attributesDescriptors.add(new AttributesDescriptor(String.format("Column Highlighting Color %d", i + 1), textAttributesKey));
        }
        DESCRIPTORS = attributesDescriptors.toArray(new AttributesDescriptor[attributesDescriptors.size()]);
    }

    public static TextAttributes getTextAttributesOfColumn(int columnIndex, UserDataHolder userDataHolder) {
        List<TextAttributes> textAttributeList = userDataHolder.getUserData(COLUMN_HIGHLIGHT_TEXT_ATTRIBUTES_KEY);
        if (textAttributeList == null) {
            EditorColorsScheme editorColorsScheme = EditorColorsManager.getInstance().getGlobalScheme();
            textAttributeList = new ArrayList<>();
            int maxIndex = 0;
            for (int colorDescriptorIndex = 0; colorDescriptorIndex < MAX_COLUMN_HIGHLIGHT_COLORS; ++colorDescriptorIndex) {
                TextAttributesKey textAttributesKey = COLUMN_HIGHLIGHT_ATTRIBUTES.get(colorDescriptorIndex);
                TextAttributes textAttributes = editorColorsScheme.getAttributes(textAttributesKey);
                textAttributeList.add(textAttributes);
                if (!textAttributesKey.getDefaultAttributes().equals(textAttributes)) {
                    maxIndex = colorDescriptorIndex;
                }
            }
            textAttributeList = textAttributeList.subList(0, maxIndex + 1);
            userDataHolder.putUserData(COLUMN_HIGHLIGHT_TEXT_ATTRIBUTES_KEY, textAttributeList);
        }
        return textAttributeList.isEmpty() ? null : textAttributeList.get(columnIndex % textAttributeList.size());
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return CsvIconProvider.FILE;
    }

    @NotNull
    @Override
    public SyntaxHighlighter getHighlighter() {
        return new CsvSyntaxHighlighter(null, null);
    }

    @NotNull
    @Override
    public String getDemoText() {
        return "1,\"Eldon Base for stackable storage shelf, platinum\",Muhammed MacIntyre,3,-213.25,38.94,35,Nunavut,Storage & Organization,0.8\n" +
                "2,\"1.7 Cubic Foot Compact \"\"Cube\"\" Office Refrigerators\",Barry French,293,457.81,208.16,68.02,Nunavut,Appliances,0.58\n" +
                "3,\"Cardinal Slant-D® Ring Binder, Heavy Gauge Vinyl\",Barry French,293,46.71,8.69,2.99,Nunavut,Binders and Binder Accessories,0.39\n" +
                "4,R380,Clay Rozendal,483,1198.97,195.99,3.99,Nunavut,Telephones and Communication,0.58\n" +
                "5,Holmes HEPA Air Purifier,Carlos Soltero,515,30.94,21.78,5.94,Nunavut,Appliances,0.5";
    }

    @Nullable
    @Override
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }

    @NotNull
    @Override
    public AttributesDescriptor[] getAttributeDescriptors() {
        return DESCRIPTORS;
    }

    @NotNull
    @Override
    public ColorDescriptor[] getColorDescriptors() {
        return new ColorDescriptor[0];
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return CsvLanguage.INSTANCE.getDisplayName();
    }
}