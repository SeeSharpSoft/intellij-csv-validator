package net.seesharpsoft.intellij.plugins.csv.editor.table.api;

import com.intellij.util.ArrayUtil;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class TableDataHandler {
    public static final int MAX_SIZE = 100;

    protected final int maxSize;
    protected final CsvTableEditor tableEditor;
    protected final Set<TableDataChangeEvent.Listener> eventListeners;

    protected List<Object[][]> states;
    protected int currentStateIndex = -1;

    public TableDataHandler(CsvTableEditor csvTableEditor, int maxSizeArg) {
        tableEditor = csvTableEditor;
        eventListeners = new HashSet<>();
        states = new ArrayList<>();
        this.maxSize = maxSizeArg;
    }

    public boolean equalsCurrentState(@NotNull Object[][] state) {
        return Arrays.deepEquals(getCurrentState(), state);
    }

    public Object[][] addState(Object[][] state) {
        if (equalsCurrentState(state)) {
            return state;
        }

        while (states.size() - 1 > currentStateIndex) {
            states.remove(states.size() - 1);
        }
        while (states.size() > maxSize - 1) {
            states.remove(0);
            --currentStateIndex;
        }
        states.add(state);
        ++currentStateIndex;
        fireStateUpdated();

        return state;
    }

    public Object[][] getCurrentState() {
        if (currentStateIndex == -1) {
            return null;
        }
        return ArrayUtil.copyOf(states.get(currentStateIndex));
    }

    public boolean canGetLastState() {
        return currentStateIndex > 0;
    }

    public boolean canGetNextState() {
        return currentStateIndex < states.size() - 1;
    }

    public Object[][] getLastState() {
        if (canGetLastState()) {
            --currentStateIndex;
            fireStateUpdated();
            return getCurrentState();
        }
        return null;
    }

    public Object[][] getNextState() {
        if (canGetNextState()) {
            ++currentStateIndex;
            fireStateUpdated();
            return getCurrentState();
        }
        return null;
    }

    public void addDataChangeListener(TableDataChangeEvent.Listener listener) {
        eventListeners.add(listener);
    }

    public void removeDataChangeListener(TableDataChangeEvent.Listener listener) {
        eventListeners.remove(listener);
    }

    protected void fireStateUpdated() {
        TableDataChangeEvent event = new TableDataChangeEvent(tableEditor, getCurrentState());
        Collections.unmodifiableSet(eventListeners).forEach(eventListener -> eventListener.onTableDataChanged(event));
    }
}
