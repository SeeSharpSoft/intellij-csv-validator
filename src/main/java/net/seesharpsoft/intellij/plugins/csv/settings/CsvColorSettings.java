package net.seesharpsoft.intellij.plugins.csv.settings;

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
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
import net.seesharpsoft.UnhandledSwitchCaseException;
import net.seesharpsoft.intellij.plugins.csv.CsvIconProvider;
import net.seesharpsoft.intellij.plugins.csv.highlighter.CsvSyntaxHighlighter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public class CsvColorSettings implements ColorSettingsPage {

    public static final TextAttributesKey COMMA =
            createTextAttributesKey("CSV_DEFAULT_COMMA", DefaultLanguageHighlighterColors.COMMA);
    public static final TextAttributesKey QUOTE =
            createTextAttributesKey("CSV_DEFAULT_QUOTE", DefaultLanguageHighlighterColors.CONSTANT);
    public static final TextAttributesKey TEXT =
            createTextAttributesKey("CSV_DEFAULT_STRING", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey ESCAPED_TEXT =
            createTextAttributesKey("CSV_ESCAPED_STRING", DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE);
    public static final TextAttributesKey COMMENT =
            createTextAttributesKey("CSV_DEFAULT_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey BAD_CHARACTER =
            createTextAttributesKey("CSV_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER);

    public static final Integer MAX_COLUMN_COLORING_COLORS = 10;
    private static final AttributesDescriptor[] DESCRIPTORS;
    private static final List<TextAttributesKey> COLUMN_COLORING_ATTRIBUTES;
    private static final Key<List<TextAttributes>> COLUMN_COLORING_TEXT_ATTRIBUTES = Key.create("CSV_PLUGIN_COLUMN_COLORING_ATTRIBUTES");

    static {
        List<AttributesDescriptor> attributesDescriptors = new ArrayList();
        attributesDescriptors.add(new AttributesDescriptor("Separator", COMMA));
        attributesDescriptors.add(new AttributesDescriptor("Quote", QUOTE));
        attributesDescriptors.add(new AttributesDescriptor("Text", TEXT));
        attributesDescriptors.add(new AttributesDescriptor("Escaped Text", ESCAPED_TEXT));
        attributesDescriptors.add(new AttributesDescriptor("Comment", COMMENT));

        COLUMN_COLORING_ATTRIBUTES = new ArrayList<>();
        for (int i = 0; i < MAX_COLUMN_COLORING_COLORS; ++i) {
            TextAttributesKey textAttributesKey = createTextAttributesKey(String.format("CSV_PLUGIN_COLUMN_COLORING_ATTRIBUTE_%d", i), TEXT);
            COLUMN_COLORING_ATTRIBUTES.add(textAttributesKey);
            attributesDescriptors.add(new AttributesDescriptor(String.format("Column Color %d", i + 1), textAttributesKey));
        }
        DESCRIPTORS = attributesDescriptors.toArray(new AttributesDescriptor[attributesDescriptors.size()]);
    }

    public static TextAttributesKey getTextAttributesKeys(int columnIndex) {
        return COLUMN_COLORING_ATTRIBUTES.get(columnIndex % 10);
    }

    public static TextAttributes getCommentTextAttributes() {
        EditorColorsScheme editorColorsScheme = EditorColorsManager.getInstance().getGlobalScheme();
        return editorColorsScheme.getAttributes(COMMENT);
    }

    public static TextAttributes getTextAttributesOfColumn(int columnIndex, UserDataHolder userDataHolder) {
        List<TextAttributes> textAttributeList = userDataHolder.getUserData(COLUMN_COLORING_TEXT_ATTRIBUTES);
        if (textAttributeList == null) {
            EditorColorsScheme editorColorsScheme = EditorColorsManager.getInstance().getGlobalScheme();
            textAttributeList = new ArrayList<>();
            int maxIndex = 0;
            switch (CsvEditorSettings.getInstance().getValueColoring()) {
                case RAINBOW:
                    maxIndex = applyColumnTextAttributes(editorColorsScheme, textAttributeList);
                    break;
                case SIMPLE:
                    textAttributeList.add(editorColorsScheme.getAttributes(TEXT));
                    break;
                default:
                    throw new UnhandledSwitchCaseException(CsvEditorSettings.getInstance().getValueColoring());
            }
            textAttributeList = textAttributeList.subList(0, maxIndex + 1);
            userDataHolder.putUserData(COLUMN_COLORING_TEXT_ATTRIBUTES, textAttributeList);
        }
        return textAttributeList.isEmpty() ? null : textAttributeList.get(columnIndex % textAttributeList.size());
    }

    private static int applyColumnTextAttributes(EditorColorsScheme editorColorsScheme, List<TextAttributes> textAttributeList) {
        int maxIndex = 0;
        TextAttributes defaultTextAttributes = editorColorsScheme.getAttributes(TEXT);
        for (int colorDescriptorIndex = 0; colorDescriptorIndex < MAX_COLUMN_COLORING_COLORS; ++colorDescriptorIndex) {
            TextAttributesKey textAttributesKey = COLUMN_COLORING_ATTRIBUTES.get(colorDescriptorIndex);
            TextAttributes textAttributes = editorColorsScheme.getAttributes(textAttributesKey);
            textAttributeList.add(textAttributes);
            if (!textAttributes.equals(defaultTextAttributes)) {
                maxIndex = colorDescriptorIndex;
            }
        }
        return maxIndex;
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
                "#4,R380,Clay Rozendal,483,1198.97,195.99,3.99,Nunavut,Telephones and Communication,0.58\n" +
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
        return "CSV/TSV/PSV";
    }
}