package net.seesharpsoft.intellij.plugins.csv.highlighter;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.tree.IElementType;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.CsvLexerFactory;
import net.seesharpsoft.intellij.plugins.csv.components.CsvValueSeparator;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
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
        CsvValueSeparator separator = CsvHelper.getValueSeparator(myProject, myVirtualFile);
        Lexer lexer = CsvLexerFactory.getInstance().createLexer(separator, CsvHelper.getEscapeCharacter(myProject, myVirtualFile));
        return new CsvHighlightingLexer(lexer, getSeparatorElement(separator));
    }

    /**
     * Resolved once per lexer, as the lexer already fixes the file's separator; settings changes reparse open files.
     */
    private static IElementType getSeparatorElement(@NotNull CsvValueSeparator separator) {
        return separator.equals(CsvValueSeparator.TAB) && CsvEditorSettings.getInstance().isHighlightTabSeparator() ?
                CsvHighlightingElement.TabSeparator.INSTANCE :
                CsvHighlightingElement.TokenBased.COMMA;
    }

    @NotNull
    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        if (tokenType instanceof CsvHighlightingElement) {
            return ((CsvHighlightingElement) tokenType).getTextAttributesKeys();
        }
        return TextAttributesKey.EMPTY_ARRAY;
    }
}
