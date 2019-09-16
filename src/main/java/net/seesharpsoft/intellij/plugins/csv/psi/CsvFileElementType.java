package net.seesharpsoft.intellij.plugins.csv.psi;

import com.intellij.lang.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import net.seesharpsoft.intellij.lang.FileParserDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CsvFileElementType extends IFileElementType {
    public CsvFileElementType(@Nullable Language language) {
        super(language);
    }

    @Override
    protected ASTNode doParseContents(@NotNull ASTNode chameleon, @NotNull PsiElement psi) {
        PsiFile file = (PsiFile) psi;
        Project project = file.getProject();
        Language languageForParser = this.getLanguageForParser(file);
        FileParserDefinition parserDefinition = (FileParserDefinition) LanguageParserDefinitions.INSTANCE.forLanguage(languageForParser);
        PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon, parserDefinition.createLexer(file), languageForParser, chameleon.getChars());
        PsiParser parser = parserDefinition.createParser(file);
        ASTNode node = parser.parse(this, builder);
        return node.getFirstChildNode();
    }
}
