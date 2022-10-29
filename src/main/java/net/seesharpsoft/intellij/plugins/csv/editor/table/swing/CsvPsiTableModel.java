package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.psi.PsiElement;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditor;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableModelBase;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.psi.PsiFileHolder;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.HashMap;
import java.util.Map;

public class CsvPsiTableModel extends CsvTableModelBase<CsvTableEditor> implements TableModel {

    /** List of listeners */
    protected EventListenerList listenerList = new EventListenerList();

    public CsvPsiTableModel(@NotNull CsvTableEditor psiFileHolder) {
        super(psiFileHolder);
    }

    @Override
    public void notifyUpdate() {
        super.notifyUpdate();
        fireTableChanged(new TableModelEvent(this));
    }

    protected void fireTableChanged(TableModelEvent e) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == TableModelListener.class) {
                ((TableModelListener)listeners[i+1]).tableChanged(e);
            }
        }
    }

    @Override
    public String getValueAt(int rowIndex, int columnIndex) {
        return super.getValueAt(rowIndex, columnIndex);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        setValueAt(aValue == null ? "" : aValue.toString(), rowIndex, columnIndex);
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        listenerList.add(TableModelListener.class, l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        listenerList.remove(TableModelListener.class, l);
    }

    @Override
    public String getColumnName(int column) {
        PsiElement headerField = getFieldAt(0, column);
        String headerText = headerField == null ? "" : CsvHelper.unquoteCsvValue(headerField.getText(), getEscapeCharacter()).trim();

        Map<String, Object> params = new HashMap<>();
        params.put("header", headerText);
        params.put("index", CsvEditorSettings.getInstance().isZeroBasedColumnNumbering() ? column : column + 1);

        return CsvHelper.formatString("${header} (${index})", params);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return Object.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return getPsiFileHolder().isEditable();
    }
}
