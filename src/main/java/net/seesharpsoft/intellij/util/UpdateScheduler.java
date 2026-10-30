package net.seesharpsoft.intellij.util;

public interface UpdateScheduler {
    void cancel();

    void schedule(Runnable update, int delayMillis);
}
