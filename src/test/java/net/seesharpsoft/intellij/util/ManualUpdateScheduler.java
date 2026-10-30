package net.seesharpsoft.intellij.util;

import java.util.ArrayList;
import java.util.List;

public final class ManualUpdateScheduler implements UpdateScheduler {
    private record Request(Runnable update, long dueAt) {
    }

    private final List<Request> requests = new ArrayList<>();
    private long now;

    @Override
    public void cancel() {
        requests.clear();
    }

    @Override
    public void schedule(Runnable update, int delayMillis) {
        requests.add(new Request(update, now + delayMillis));
    }

    public int pendingCount() {
        return requests.size();
    }

    public Runnable pendingUpdate() {
        if (requests.size() != 1) throw new IllegalStateException("Expected exactly one pending refresh");
        return requests.getFirst().update();
    }

    public void advanceBy(int millis) {
        now += millis;
        while (!requests.isEmpty() && requests.getFirst().dueAt() <= now) {
            requests.removeFirst().update().run();
        }
    }
}
