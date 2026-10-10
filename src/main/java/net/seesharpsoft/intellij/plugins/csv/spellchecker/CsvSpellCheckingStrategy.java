package net.seesharpsoft.intellij.plugins.csv.spellchecker;

import com.intellij.psi.PsiElement;
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy;
import com.intellij.spellchecker.tokenizer.Tokenizer;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvField;
import org.jetbrains.annotations.NotNull;

public class CsvSpellCheckingStrategy extends SpellcheckingStrategy {
    private static final Tokenizer<PsiElement> CSV_FIELD_TOKENIZER = new SynchronizedTokenizer<>(TEXT_TOKENIZER);

    @Override
    public @NotNull Tokenizer getTokenizer(PsiElement element) {
        if (element instanceof CsvField) {
            return CSV_FIELD_TOKENIZER;
        }
        return EMPTY_TOKENIZER;
    }
}
