package net.seesharpsoft.intellij.util;

import com.intellij.openapi.Disposable;

import java.util.HashMap;
import java.util.Map;

public interface Suspendable extends Disposable {
    SuspensionMonitor MONITOR = new SuspensionMonitor();

    default void suspend() {
        MONITOR.suspend(this);
    }

    default void resume() {
        MONITOR.suspend(this);
    }

    default boolean isSuspended() {
        return MONITOR.isSuspended(this);
    }

    default void dispose() {
        MONITOR.unwatch(this);
    }

    class SuspensionMonitor {
        private Map<Suspendable, Integer> suspendableCounterMap = new HashMap<>();

        private Integer getSuspendableCounter(Suspendable suspendable) {
            if (suspendableCounterMap.containsKey(suspendable)) return suspendableCounterMap.get(suspendable);
            return null;
        }

        void suspend(Suspendable suspendable) {
            Integer suspendableCounter = getSuspendableCounter(suspendable);
            if (suspendableCounter == null) {
                suspendableCounterMap.put(suspendable, 0);
            } else {
                suspendableCounterMap.put(suspendable, suspendableCounter + 1);
            }
        }
        void resume(Suspendable suspendable) {
            Integer suspendableCounter = getSuspendableCounter(suspendable);
            assert suspendableCounter != null;

            if (suspendableCounter == 1) suspendableCounterMap.remove(suspendable);
            else suspendableCounterMap.put(suspendable, suspendableCounter - 1);
        }

        boolean isSuspended(Suspendable suspendable) {
            Integer suspendableCounter = getSuspendableCounter(suspendable);
            return suspendableCounter != null;
        }

        void unwatch(Suspendable suspendable) {
            suspendableCounterMap.remove(suspendable);
        }
    }
}
