package net.seesharpsoft.intellij.plugins.tsv;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public final class TsvFileType extends LanguageFileType {
    public static final TsvFileType INSTANCE = new TsvFileType();

    public static final Icon ICON = IconLoader.getIcon("/media/icons/tsv-icon.png", TsvFileType.class);

    private TsvFileType() {
        super(TsvLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "TSV";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "TSV file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "tsv";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return ICON;
    }
}