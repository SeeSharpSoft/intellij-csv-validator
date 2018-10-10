package net.seesharpsoft.intellij.plugins.csv.editor.table.api;

import com.intellij.util.ArrayUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;

import java.util.EventListener;
import java.util.EventObject;

public class TableDataChangeEvent extends EventObject {

    public interface Listener extends EventListener {
        void onTableDataChanged(TableDataChangeEvent event);
    }

    private Object[][] value;

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public TableDataChangeEvent(Object source, Object[][] values) {
        super(source);
        this.value = values;
    }

    public Object[][] getValue() {
        return CsvHelper.deepCopy(value);
    }
}
