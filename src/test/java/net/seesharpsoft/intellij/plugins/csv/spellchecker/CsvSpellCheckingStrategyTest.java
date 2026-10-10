package net.seesharpsoft.intellij.plugins.csv.spellchecker;

import com.intellij.psi.PsiElement;
import com.intellij.spellchecker.tokenizer.TokenConsumer;
import com.intellij.spellchecker.tokenizer.Tokenizer;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvField;
import org.junit.Test;

import java.util.ConcurrentModificationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class CsvSpellCheckingStrategyTest {
    @Test
    public void csvFieldsUseTheGuardedTokenizer() {
        Tokenizer<?> tokenizer = new CsvSpellCheckingStrategy().getTokenizer(mock(CsvField.class));

        assertTrue(tokenizer instanceof SynchronizedTokenizer);
    }

    @Test
    public void concurrentTokenizationDoesNotPropagateConcurrentModificationException() throws Exception {
        CountDownLatch firstCallStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstCall = new CountDownLatch(1);
        CountDownLatch concurrentCallReachedDelegate = new CountDownLatch(1);
        AtomicBoolean delegateInUse = new AtomicBoolean();
        Tokenizer<PsiElement> unsafeTokenizer = new Tokenizer<>() {
            @Override
            public void tokenize(PsiElement element, TokenConsumer consumer) {
                if (!delegateInUse.compareAndSet(false, true)) {
                    concurrentCallReachedDelegate.countDown();
                    throw new ConcurrentModificationException("Grazie tokenizer state was accessed concurrently");
                }
                firstCallStarted.countDown();
                try {
                    releaseFirstCall.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                } finally {
                    delegateInUse.set(false);
                }
            }
        };
        SynchronizedTokenizer<PsiElement> tokenizer = new SynchronizedTokenizer<>(unsafeTokenizer);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> firstCall = executor.submit(() -> tokenizer.tokenize(null, null));
            assertTrue(firstCallStarted.await(5, TimeUnit.SECONDS));

            Future<?> secondCall = executor.submit(() -> tokenizer.tokenize(null, null));
            assertFalse(
                    "The second spell-checking call must wait for the first call to finish",
                    concurrentCallReachedDelegate.await(200, TimeUnit.MILLISECONDS)
            );

            releaseFirstCall.countDown();
            firstCall.get(5, TimeUnit.SECONDS);
            secondCall.get(5, TimeUnit.SECONDS);
        } finally {
            releaseFirstCall.countDown();
            executor.shutdownNow();
        }
    }
}
