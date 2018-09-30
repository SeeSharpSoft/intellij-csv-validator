package net.seesharpsoft.intellij.plugins.csv.editor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public abstract class CsvTableEditorUtilBase implements PropertyChangeListener {

    protected CsvTableEditor csvTableEditor;

    public CsvTableEditorUtilBase(CsvTableEditor csvTableEditorArg) {
        this.csvTableEditor = csvTableEditorArg;
        this.csvTableEditor.addPropertyChangeListener(this);
    }

    protected abstract void onEditorUpdated();

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case CsvTableEditor.CHANGE_EVENT_TABLE_UPDATE:
                onEditorUpdated();
                break;
        }
    }
}
