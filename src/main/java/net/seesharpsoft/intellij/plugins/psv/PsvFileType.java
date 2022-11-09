package net.seesharpsoft.intellij.plugins.psv;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.IconLoader;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import net.seesharpsoft.intellij.plugins.csv.CsvSeparatorHolder;
import net.seesharpsoft.intellij.plugins.csv.CsvValueSeparator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public final class PsvFileType extends LanguageFileType implements CsvSeparatorHolder {
    public static final PsvFileType INSTANCE = new PsvFileType();

    public static final Icon ICON = IconLoader.getIcon("/media/icons/psv-icon.png", PsvFileType.class);

    private PsvFileType() {
        super(CsvLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "PSV";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "PSV (Pipe Separated Values)";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "psv";
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
        return CsvValueSeparator.PIPE;
    }
}