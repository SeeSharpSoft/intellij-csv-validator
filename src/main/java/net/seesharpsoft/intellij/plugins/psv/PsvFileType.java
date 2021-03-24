package net.seesharpsoft.intellij.plugins.psv;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public final class PsvFileType extends LanguageFileType {
    public static final PsvFileType INSTANCE = new PsvFileType();

    public static final Icon ICON = IconLoader.getIcon("/media/icons/psv-icon.png", PsvFileType.class);

    private PsvFileType() {
        super(PsvLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "PSV";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "PSV file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "psv";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return ICON;
    }
}