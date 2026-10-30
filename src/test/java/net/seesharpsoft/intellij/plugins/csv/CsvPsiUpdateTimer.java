package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiTreeChangeAdapter;
import com.intellij.psi.PsiTreeChangeEvent;
import com.intellij.psi.impl.PsiTreeChangeEventImpl;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Measures the time the platform spends updating PSI, e.g. when committing a changed document. The update fires one
 * PSI event per changed element; its duration depends on the machine and on the PSI listeners of other plugins, such as
 * Kotlin, so performance budgets of the plugin exclude it.
 */
public final class CsvPsiUpdateTimer extends PsiTreeChangeAdapter {
    private final AtomicLong totalNanos = new AtomicLong();
    private final AtomicLong untakenNanos = new AtomicLong();
    private int depth;
    private long started;

    private CsvPsiUpdateTimer() {
    }

    public static CsvPsiUpdateTimer start(@NotNull Project project, @NotNull Disposable parentDisposable) {
        CsvPsiUpdateTimer timer = new CsvPsiUpdateTimer();
        PsiManager.getInstance(project).addPsiTreeChangeListener(timer, parentDisposable);
        return timer;
    }

    public long getTotalMillis() {
        return TimeUnit.NANOSECONDS.toMillis(totalNanos.get());
    }

    /**
     * Returns the PSI update time since the previous call.
     */
    public long takeNanos() {
        return untakenNanos.getAndSet(0);
    }

    // Each PSI update is wrapped in generic change events.
    @Override
    public void beforeChildrenChange(@NotNull PsiTreeChangeEvent event) {
        if (isGeneric(event) && depth++ == 0) started = System.nanoTime();
    }

    @Override
    public void childrenChanged(@NotNull PsiTreeChangeEvent event) {
        if (isGeneric(event) && depth > 0 && --depth == 0) {
            long elapsed = System.nanoTime() - started;
            totalNanos.addAndGet(elapsed);
            untakenNanos.addAndGet(elapsed);
        }
    }

    private static boolean isGeneric(PsiTreeChangeEvent event) {
        return event instanceof PsiTreeChangeEventImpl impl && impl.isGenericChange();
    }
}
