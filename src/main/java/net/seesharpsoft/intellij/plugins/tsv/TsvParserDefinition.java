package net.seesharpsoft.intellij.plugins.tsv;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import net.seesharpsoft.intellij.plugins.csv.CsvLexerAdapter;
import net.seesharpsoft.intellij.plugins.csv.CsvParserDefinition;
import net.seesharpsoft.intellij.plugins.csv.formatter.CsvCodeStyleSettings;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import org.jetbrains.annotations.NotNull;

public class TsvParserDefinition extends CsvParserDefinition {
    public static final IFileElementType TSV_FILE = new IFileElementType(TsvLanguage.INSTANCE);
    
    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        return new CsvLexerAdapter(CsvCodeStyleSettings.TAB_SEPARATOR);
    }
    
    @Override
    public IFileElementType getFileNodeType() {
        return TSV_FILE;
    }

    @Override
    public PsiFile createFile(FileViewProvider viewProvider) {
        return new CsvFile(viewProvider, TsvFileType.INSTANCE);
    }
}