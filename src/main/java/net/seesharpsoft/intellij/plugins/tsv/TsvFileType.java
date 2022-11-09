package net.seesharpsoft.intellij.plugins.tsv;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.IconLoader;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import net.seesharpsoft.intellij.plugins.csv.CsvSeparatorHolder;
import net.seesharpsoft.intellij.plugins.csv.CsvValueSeparator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public final class TsvFileType extends LanguageFileType implements CsvSeparatorHolder {
    public static final TsvFileType INSTANCE = new TsvFileType();

    public static final Icon ICON = IconLoader.getIcon("/media/icons/tsv-icon.png", TsvFileType.class);

    private TsvFileType() {
        super(CsvLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "TSV";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "TSV/TAB (Tab separated Values)";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "tsv";
    }

    @Override
    public @NotNull String getDisplayName() {
        return getName() + " (CSV Editor)";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return ICON;
    }

    @Override
    public CsvValueSeparator getSeparator() {
        return CsvValueSeparator.TAB;
    }
}