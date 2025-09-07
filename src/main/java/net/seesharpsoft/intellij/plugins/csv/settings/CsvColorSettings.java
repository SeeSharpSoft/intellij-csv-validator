package net.seesharpsoft.intellij.plugins.csv.settings;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import net.seesharpsoft.intellij.plugins.csv.CsvIconProvider;
import net.seesharpsoft.intellij.plugins.csv.highlighter.CsvSyntaxHighlighter;
import net.seesharpsoft.intellij.plugins.csv.highlighter.CsvTextAttributeKeys;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;

import static net.seesharpsoft.intellij.plugins.csv.CsvPluginManager.getLocalizedText;

public class CsvColorSettings implements ColorSettingsPage {
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
        return """
                header1,header2,header3,header4,header5,header6,header7,header8,header9,header10,header11
                1,2,3,4,5,6,7,8,9,10,11
                value1,value2,value3,value4,value5,value6,value7,value8,value9,value10,value11
                """;
    }

    @Nullable
    @Override
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }

    @NotNull
    @Override
    public AttributesDescriptor[] getAttributeDescriptors() {
        return CsvTextAttributeKeys.DESCRIPTORS;
    }

    @NotNull
    @Override
    public ColorDescriptor[] getColorDescriptors() {
        return new ColorDescriptor[0];
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return getLocalizedText("settings.title");
    }
}