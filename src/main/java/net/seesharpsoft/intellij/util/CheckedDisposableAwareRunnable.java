package net.seesharpsoft.intellij.util;

import com.intellij.openapi.util.CheckedDisposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

public class CheckedDisposableAwareRunnable<T extends Runnable> extends WeakReference<CheckedDisposable> implements Runnable {
    protected final T myDelegate;

    private CheckedDisposableAwareRunnable(@NotNull T delegate, @NotNull CheckedDisposable disposable) {
        super(disposable);
        myDelegate = delegate;
    }

    @NotNull
    public static Runnable create(@NotNull Runnable delegate, @Nullable CheckedDisposable disposable) {
        if (disposable == null) {
            return delegate;
        }

        return new CheckedDisposableAwareRunnable<>(delegate, disposable);
    }

    @Override
    public void run() {
        CheckedDisposable res = get();
        if (res == null || res.isDisposed()) return;

        myDelegate.run();
    }
}
