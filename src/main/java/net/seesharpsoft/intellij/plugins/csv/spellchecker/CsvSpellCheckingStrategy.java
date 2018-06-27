package net.seesharpsoft.intellij.plugins.csv.spellchecker;

import com.intellij.psi.PsiElement;
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy;
import com.intellij.spellchecker.tokenizer.Tokenizer;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvField;

public class CsvSpellCheckingStrategy extends SpellcheckingStrategy {
    @Override
    public Tokenizer getTokenizer(PsiElement element) {
        if (element instanceof CsvField) {
            return TEXT_TOKENIZER;
        }
        return EMPTY_TOKENIZER;
    }
}
