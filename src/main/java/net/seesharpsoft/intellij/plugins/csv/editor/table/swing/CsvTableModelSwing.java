package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiElement;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditor;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableModelBase;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class CsvTableModelSwing extends CsvTableModelBase<CsvTableEditor> implements TableModel {

    /** List of listeners */
    protected EventListenerList listenerList = new EventListenerList();

    protected ScheduledFuture delayedUpdate;

    protected ScheduledExecutorService executorService;

    public CsvTableModelSwing(@NotNull CsvTableEditor psiFileHolder) {
        super(psiFileHolder);
        executorService = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void notifyUpdate() {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            doNotifyUpdate();
            return;
        }

        if (delayedUpdate != null && !delayedUpdate.isDone()) {
            delayedUpdate.cancel(true);
        }

        delayedUpdate = executorService.schedule(() -> SwingUtilities.invokeLater(this::doNotifyUpdate), 50, TimeUnit.MILLISECONDS);
    }

    private void doNotifyUpdate() {
        getPsiFileHolder().beforeTableModelUpdate();
        try {
            super.notifyUpdate();
            fireTableChanged(new TableModelEvent(this));
        } finally {
            getPsiFileHolder().afterTableModelUpdate();
        }
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
    public Object getValueAt(int rowIndex, int columnIndex) {
        return getValue(rowIndex, columnIndex);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        setValue(aValue == null ? "" : aValue.toString(), rowIndex, columnIndex);
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
