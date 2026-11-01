package net.seesharpsoft.intellij.plugins.csv.highlighter;

import com.intellij.lexer.Lexer;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.CsvLexerFactory;
import net.seesharpsoft.intellij.plugins.csv.components.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.components.CsvValueSeparator;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class CsvHighlightingLexerTest extends CsvBasePlatformTestCase {
    @Override
    protected void tearDown() throws Exception {
        CsvEditorSettings.getInstance().loadState(new CsvEditorSettings.OptionSet());
        super.tearDown();
    }

    private CsvHighlightingLexer lexer() {
        return new CsvHighlightingLexer(CsvLexerFactory.getInstance().createLexer(
                CsvValueSeparator.COMMA, CsvEscapeCharacter.QUOTE, "#"));
    }

    public void testSettingsServiceIsNotLookedUpForEveryToken() {
        CsvEditorSettings settings = CsvEditorSettings.getInstance();
        try (MockedStatic<CsvEditorSettings> mocked = mockStatic(CsvEditorSettings.class)) {
            mocked.when(CsvEditorSettings::getInstance).thenReturn(settings);
            CsvHighlightingLexer lexer = lexer();
            lexer.start("first,second,third\n".repeat(1000));
            while (lexer.getTokenType() != null) lexer.advance();
            mocked.verify(CsvEditorSettings::getInstance, times(1));
        }
    }

    public void testTabSeparatorHighlightingIsDecidedOncePerLexerInsteadOfPerToken() {
        myFixture.configureByText("test.tsv", "");
        CsvEditorSettings settings = spy(CsvEditorSettings.getInstance());
        settings.setHighlightTabSeparator(true);
        try (MockedStatic<CsvEditorSettings> mocked = mockStatic(CsvEditorSettings.class)) {
            mocked.when(CsvEditorSettings::getInstance).thenReturn(settings);
            Lexer lexer = new CsvSyntaxHighlighter(getProject(), myFixture.getFile().getVirtualFile()).getHighlightingLexer();
            lexer.start("first\tsecond\tthird\n".repeat(1000));
            int tabSeparators = 0;
            for (; lexer.getTokenType() != null; lexer.advance()) {
                if (lexer.getTokenType() == CsvHighlightingElement.TabSeparator.INSTANCE) tabSeparators++;
            }
            assertEquals(2000, tabSeparators);
            verify(settings, atMost(1)).isHighlightTabSeparator();
        }
    }

    public void testColoringChangesStillTakeEffectWithoutRestartingLexer() {
        CsvEditorSettings settings = CsvEditorSettings.getInstance();
        settings.setValueColoring(CsvEditorSettings.ValueColoring.RAINBOW);
        CsvHighlightingLexer lexer = lexer();
        lexer.start("first,second");
        assertSame(CsvHighlightingElement.ColumnBased.forIndex(0), lexer.getTokenType());
        settings.setValueColoring(CsvEditorSettings.ValueColoring.SIMPLE);
        assertSame(CsvHighlightingElement.TokenBased.TEXT, lexer.getTokenType());
        settings.setValueColoring(CsvEditorSettings.ValueColoring.RAINBOW);
        lexer.advance();
        lexer.advance();
        assertSame(CsvHighlightingElement.ColumnBased.forIndex(1), lexer.getTokenType());
    }
}
