package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public final class CsvFileType extends LanguageFileType {
    public static final CsvFileType INSTANCE = new CsvFileType();

    private CsvFileType() {
        super(CsvLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "CSV";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "CSV (Comma Separated Values)";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "csv";
    }

    @Override
    public @NotNull String getDisplayName() {
        return getName() + " (CSV Editor)";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return CsvIconProvider.FILE;
    }
}