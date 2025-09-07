package net.seesharpsoft.intellij.plugins.csv.highlighter;

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import net.seesharpsoft.UnhandledSwitchCaseException;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;
import static net.seesharpsoft.intellij.plugins.csv.CsvPluginManager.getResourceBundle;

public final class CsvTextAttributeKeys {
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
    public static final AttributesDescriptor[] DESCRIPTORS;
    private static final List<TextAttributesKey> COLUMN_COLORING_ATTRIBUTES;
    private static final Key<List<TextAttributes>> COLUMN_COLORING_TEXT_ATTRIBUTES = Key.create("CSV_PLUGIN_COLUMN_COLORING_ATTRIBUTES");

    static {
        List<AttributesDescriptor> attributesDescriptors = new ArrayList<>();
        ResourceBundle bundle = getResourceBundle();
        attributesDescriptors.add(new AttributesDescriptor(bundle.getString("color.attribute.separator"), CsvTextAttributeKeys.COMMA));
        attributesDescriptors.add(new AttributesDescriptor(bundle.getString("color.attribute.quote"), CsvTextAttributeKeys.QUOTE));
        attributesDescriptors.add(new AttributesDescriptor(bundle.getString("color.attribute.text"), CsvTextAttributeKeys.TEXT));
        attributesDescriptors.add(new AttributesDescriptor(bundle.getString("color.attribute.text.escaped"), CsvTextAttributeKeys.ESCAPED_TEXT));
        attributesDescriptors.add(new AttributesDescriptor(bundle.getString("color.attribute.comment"), CsvTextAttributeKeys.COMMENT));

        COLUMN_COLORING_ATTRIBUTES = new ArrayList<>();
        for (int i = 0; i < MAX_COLUMN_COLORING_COLORS; ++i) {
            TextAttributesKey textAttributesKey = createTextAttributesKey(String.format("CSV_PLUGIN_COLUMN_COLORING_ATTRIBUTE_%d", i), CsvTextAttributeKeys.TEXT);
            COLUMN_COLORING_ATTRIBUTES.add(textAttributesKey);
            attributesDescriptors.add(new AttributesDescriptor(String.format(bundle.getString("color.attribute.column.nr"), i + 1), textAttributesKey));
        }
        DESCRIPTORS = attributesDescriptors.toArray(new AttributesDescriptor[0]);
    }

    public static TextAttributesKey getTextAttributesKeys(int columnIndex) {
        return COLUMN_COLORING_ATTRIBUTES.get(columnIndex % 10);
    }

    public static TextAttributes getCommentTextAttributes() {
        EditorColorsScheme editorColorsScheme = EditorColorsManager.getInstance().getGlobalScheme();
        return editorColorsScheme.getAttributes(CsvTextAttributeKeys.COMMENT);
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
                    textAttributeList.add(editorColorsScheme.getAttributes(CsvTextAttributeKeys.TEXT));
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
        TextAttributes defaultTextAttributes = editorColorsScheme.getAttributes(CsvTextAttributeKeys.TEXT);
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

    private CsvTextAttributeKeys() {
        // static utility class
    }
}
