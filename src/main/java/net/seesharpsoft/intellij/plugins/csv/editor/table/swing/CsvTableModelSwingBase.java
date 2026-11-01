package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.util.Computable;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;

import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.HashMap;
import java.util.Map;

public abstract class CsvTableModelSwingBase implements TableModel {
    private final EventListenerList listeners = new EventListenerList();

    public abstract String getValue(int row, int column);

    public abstract void setValue(String value, int row, int column);

    public abstract boolean isCommentRow(int row);

    protected <T> T readModel(Computable<T> computation) {
        return computation.compute();
    }

    protected String getHeaderText(int column) {
        for (int row = 0; row < getRowCount(); row++) {
            if (!isCommentRow(row)) return getValue(row, column).trim();
        }
        return "";
    }

    @Override
    public String getColumnName(int column) {
        return readModel(() -> {
            Map<String, Object> params = new HashMap<>();
            params.put("header", getHeaderText(column));
            params.put("index", CsvEditorSettings.getInstance().isZeroBasedColumnNumbering() ? column : column + 1);
            return CsvHelper.formatString("${header} (${index})", params);
        });
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return Object.class;
    }

    @Override
    public Object getValueAt(int row, int column) {
        return getValue(row, column);
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        setValue(value == null ? "" : value.toString(), row, column);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    @Override
    public void addTableModelListener(TableModelListener listener) {
        listeners.add(TableModelListener.class, listener);
    }

    @Override
    public void removeTableModelListener(TableModelListener listener) {
        listeners.remove(TableModelListener.class, listener);
    }

    protected void fireTableChanged(TableModelEvent event) {
        for (TableModelListener listener : listeners.getListeners(TableModelListener.class))
            listener.tableChanged(event);
    }

    public void fireTableDataChanged() {
        fireTableChanged(new TableModelEvent(this));
    }

    public void fireTableStructureChanged() {
        fireTableChanged(new TableModelEvent(this, TableModelEvent.HEADER_ROW));
    }
}
