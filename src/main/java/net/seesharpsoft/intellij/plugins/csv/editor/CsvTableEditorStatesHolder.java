package net.seesharpsoft.intellij.plugins.csv.editor;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CsvTableEditorStatesHolder extends CsvTableEditorUtilBase {
    public static final int MAX_SIZE = 100;

    private int maxSize = MAX_SIZE;
    private List<Object[][]> states;
    private int currentStateIndex = -1;

    public CsvTableEditorStatesHolder(CsvTableEditor csvTableEditor, int maxSizeArg) {
        super(csvTableEditor);
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
        while (states.size() > maxSize) {
            states.remove(0);
            --currentStateIndex;
        }
        states.add(state);
        ++currentStateIndex;
        onStateUpdated();

        return state;
    }

    public Object[][] getCurrentState() {
        if (currentStateIndex == -1) {
            return null;
        }
        return states.get(currentStateIndex);
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
            onStateUpdated();
            return getCurrentState();
        }
        return null;
    }

    public Object[][] getNextState() {
        if (canGetNextState()) {
            ++currentStateIndex;
            onStateUpdated();
            return getCurrentState();
        }
        return null;
    }

    private void onStateUpdated() {
        csvTableEditor.updateUndoRedoButtonsEnabled();
    }

    @Override
    protected void onEditorUpdated() {

    }
}
