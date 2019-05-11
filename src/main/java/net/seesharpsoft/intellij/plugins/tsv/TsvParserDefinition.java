package net.seesharpsoft.intellij.plugins.tsv;

import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import net.seesharpsoft.intellij.plugins.csv.CsvParserDefinition;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFileElementType;

public class TsvParserDefinition extends CsvParserDefinition {
    public static final IFileElementType TSV_FILE = new CsvFileElementType(TsvLanguage.INSTANCE);

    @Override
    public IFileElementType getFileNodeType() {
        return TSV_FILE;
    }

    @Override
    public PsiFile createFile(FileViewProvider viewProvider) {
        return new CsvFile(viewProvider, TsvFileType.INSTANCE);
    }
}
