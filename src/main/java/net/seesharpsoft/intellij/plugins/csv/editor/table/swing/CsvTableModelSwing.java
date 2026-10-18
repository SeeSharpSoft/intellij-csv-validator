package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiElement;
import com.intellij.util.Alarm;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditor;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableModelBase;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvField;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.psi.PsiHelper;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.HashMap;
import java.util.Map;

public class CsvTableModelSwing extends CsvTableModelBase<CsvTableEditor> implements TableModel {

    private final EventListenerList listenerList = new EventListenerList();
    private final Alarm myUpdateAlarm;

    private static final int UPDATE_DELAY_MILLIS = 50;

    public CsvTableModelSwing(@NotNull CsvTableEditor psiFileHolder) {
        super(psiFileHolder);
        myUpdateAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, psiFileHolder);
    }

    @Override
    public void notifyUpdate() {
        if (isDisposed()) return;

        if (ApplicationManager.getApplication().isUnitTestMode()) {
            updateTableImmediately();
            return;
        }

        myUpdateAlarm.cancelAllRequests();
        myUpdateAlarm.addRequest(this::updateTableImmediately, UPDATE_DELAY_MILLIS);
    }

    private void updateTableImmediately() {
        if (isDisposed()) return;

        getPsiFileHolder().beforeTableModelUpdate();
        try {
            super.notifyUpdate();
            fireTableChanged(new TableModelEvent(this));
        } finally {
            getPsiFileHolder().afterTableModelUpdate();
        }
    }

    protected void fireTableChanged(TableModelEvent e) {
        for (TableModelListener listener : listenerList.getListeners(TableModelListener.class)) {
            listener.tableChanged(e);
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

    /**
     * Gets header name for a given column, with index formatting.
     * PSI access is wrapped in a read action for thread safety.
     */
    @Override
    public String getColumnName(int column) {
        return ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
            return formatColumnName(column, getHeaderText(column));
        });
    }

    private String getHeaderText(int column) {
        PsiElement headerField = PsiHelper.findFirst(getPsiFile(), CsvTypes.FIELD);
        if (headerField == null) return "";

        PsiElement columnField = PsiHelper.getNextNthSiblingOfType(headerField, column, CsvField.class);
        return columnField == null
                ? ""
                : CsvHelper.unquoteCsvValue(columnField.getText(), getEscapeCharacter()).trim();
    }

    private String formatColumnName(int column, String headerText) {
        Map<String, Object> params = new HashMap<>();
        params.put("header", headerText);
        params.put("index", getDisplayedColumnNumber(column));
        return CsvHelper.formatString("${header} (${index})", params);
    }

    private int getDisplayedColumnNumber(int column) {
        return CsvEditorSettings.getInstance().isZeroBasedColumnNumbering() ? column : column + 1;
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
