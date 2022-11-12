package net.seesharpsoft.intellij.util;

import com.intellij.openapi.Disposable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface Suspendable extends Disposable {
    SuspensionMonitor MONITOR = new SuspensionMonitor();

    default void suspend() {
        MONITOR.suspend(this);
    }

    default void resume() {
        MONITOR.resume(this);
    }

    default boolean isSuspended() {
        return MONITOR.isSuspended(this);
    }

    default void dispose() {
        MONITOR.unwatch(this);
    }

    class SuspensionMonitor {
        private final Map<Suspendable, Integer> suspendableCounterMap = new ConcurrentHashMap<>();

        private Integer getSuspendableCounter(Suspendable suspendable) {
            if (suspendableCounterMap.containsKey(suspendable)) return suspendableCounterMap.get(suspendable);
            return null;
        }

        synchronized void suspend(Suspendable suspendable) {
            Integer suspendableCounter = getSuspendableCounter(suspendable);
            if (suspendableCounter == null) {
                suspendableCounterMap.put(suspendable, 1);
            } else {
                suspendableCounterMap.put(suspendable, suspendableCounter + 1);
            }
        }

        synchronized void resume(Suspendable suspendable) {
            Integer suspendableCounter = getSuspendableCounter(suspendable);
            // this usually shouldn't happen but doesn't hurt, so fail gracefully
            if (suspendableCounter == null) return;

            if (suspendableCounter == 1) suspendableCounterMap.remove(suspendable);
            else suspendableCounterMap.put(suspendable, suspendableCounter - 1);
        }

        boolean isSuspended(Suspendable suspendable) {
            Integer suspendableCounter = getSuspendableCounter(suspendable);
            return suspendableCounter != null;
        }

        synchronized void unwatch(Suspendable suspendable) {
            suspendableCounterMap.remove(suspendable);
        }
    }
}
