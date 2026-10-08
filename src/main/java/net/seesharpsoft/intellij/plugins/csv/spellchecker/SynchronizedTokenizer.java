package net.seesharpsoft.intellij.plugins.csv.spellchecker;

import com.intellij.psi.PsiElement;
import com.intellij.spellchecker.tokenizer.TokenConsumer;
import com.intellij.spellchecker.tokenizer.Tokenizer;

final class SynchronizedTokenizer<T extends PsiElement> extends Tokenizer<T> {
    private final Tokenizer<T> delegate;

    SynchronizedTokenizer(Tokenizer<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public synchronized void tokenize(T element, TokenConsumer consumer) {
        delegate.tokenize(element, consumer);
    }
}
