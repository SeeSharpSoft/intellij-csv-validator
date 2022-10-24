package net.seesharpsoft.intellij.plugins.csv.highlighter;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.tree.IElementType;
import net.seesharpsoft.intellij.plugins.csv.*;
import org.jetbrains.annotations.NotNull;

public class CsvSyntaxHighlighter extends SyntaxHighlighterBase {
    private final Project myProject;
    private final VirtualFile myVirtualFile;

    public CsvSyntaxHighlighter(Project project, VirtualFile virtualFile) {
        this.myProject = project;
        this.myVirtualFile = virtualFile;
    }

    @NotNull
    @Override
    public Lexer getHighlightingLexer() {
        return new CsvHighlightingLexer(CsvLexerFactory.getInstance().createLexer(myProject, myVirtualFile));
    }

    @NotNull
    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        return ((CsvHighlightingElement)tokenType).getTextAttributesKeys();
    }
}
