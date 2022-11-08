package net.seesharpsoft.intellij.plugins.csv.psi;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NlsSafe;
import net.seesharpsoft.intellij.plugins.csv.*;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class CsvPsiParserFileType extends LanguageFileType implements CsvSeparatorHolder, CsvEscapeCharacterHolder {
    private CsvValueSeparator mySeparator;
    private CsvEscapeCharacter myEscapeCharacter;

    protected CsvPsiParserFileType(@NotNull CsvValueSeparator valueSeparator, @NotNull CsvEscapeCharacter escapeCharacter) {
        super(CsvLanguage.INSTANCE);
        mySeparator = valueSeparator;
        myEscapeCharacter = escapeCharacter;
    }

    @Override
    public @NonNls @NotNull String getName() {
        return "CsvPsiParserFileType";
    }

    @Override
    public @NlsContexts.Label @NotNull String getDescription() {
        return "Dummy file type for d";
    }

    @Override
    public @NlsSafe @NotNull String getDefaultExtension() {
        return "csv";
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public CsvEscapeCharacter getEscapeCharacter() {
        return myEscapeCharacter;
    }

    public void setEscapeCharacter(CsvEscapeCharacter escapeCharacter) {
        myEscapeCharacter = escapeCharacter;
    }

    @Override
    public CsvValueSeparator getSeparator() {
        return mySeparator;
    }

    public void setSeparator(CsvValueSeparator separator) {
        mySeparator = separator;
    }
}
