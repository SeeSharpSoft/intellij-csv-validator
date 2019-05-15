package net.seesharpsoft.intellij.plugins.psv;

import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import net.seesharpsoft.intellij.plugins.csv.CsvParserDefinition;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFileElementType;

public class PsvParserDefinition extends CsvParserDefinition {
    public static final IFileElementType PSV_FILE = new CsvFileElementType(PsvLanguage.INSTANCE);

    @Override
    public IFileElementType getFileNodeType() {
        return PSV_FILE;
    }

    @Override
    public PsiFile createFile(FileViewProvider viewProvider) {
        return new CsvFile(viewProvider, PsvFileType.INSTANCE);
    }
}
