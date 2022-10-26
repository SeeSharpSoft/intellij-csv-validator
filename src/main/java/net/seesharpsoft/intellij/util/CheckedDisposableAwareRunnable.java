package net.seesharpsoft.intellij.util;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

public class CheckedDisposableAwareRunnable<T extends Runnable> extends WeakReference<Disposable> implements Runnable {
    protected final T myDelegate;

    private CheckedDisposableAwareRunnable(@NotNull T delegate, @NotNull Disposable disposable) {
        super(disposable);
        myDelegate = delegate;
    }

    @NotNull
    public static Runnable create(@NotNull Runnable delegate, @Nullable Disposable disposable) {
        if (disposable == null) {
            return delegate;
        }

        return new CheckedDisposableAwareRunnable<>(delegate, disposable);
    }

    @Override
    public void run() {
        Disposable res = get();
        if (res == null || Disposer.isDisposed(res)) return;

        myDelegate.run();
    }
}
