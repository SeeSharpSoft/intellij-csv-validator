package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.util.Alarm;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditor;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableModelBase;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.util.UpdateScheduler;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CsvTableModelSwing extends CsvTableModelBase<CsvTableEditor> implements TableModel {

    private final EventListenerList listenerList = new EventListenerList();
    private final UpdateScheduler updateScheduler;
    private ModelState preparedState;
    private List<String> headerTexts;

    private static final int UPDATE_DELAY_MILLIS = 50;

    public CsvTableModelSwing(@NotNull CsvTableEditor psiFileHolder) {
        this(psiFileHolder, createUpdateScheduler(psiFileHolder));
    }

    CsvTableModelSwing(@NotNull CsvTableEditor psiFileHolder, @NotNull UpdateScheduler updateScheduler) {
        super(psiFileHolder);
        this.updateScheduler = updateScheduler;
    }

    private static UpdateScheduler createUpdateScheduler(CsvTableEditor editor) {
        Alarm alarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, editor);
        return new UpdateScheduler() {
            @Override
            public void cancel() {
                alarm.cancelAllRequests();
            }

            @Override
            public void schedule(Runnable update, int delayMillis) {
                if (ApplicationManager.getApplication().isUnitTestMode()) update.run();
                else alarm.addRequest(update, delayMillis);
            }
        };
    }

    @Override
    public void dispose() {
        super.dispose();
        updateScheduler.cancel();
        preparedState = null;
    }

    @Override
    public void notifyUpdate() {
        if (isDisposed() || isSuspended()) return;

        if (ApplicationManager.getApplication().isDispatchThread() &&
                getPsiFileHolder() instanceof CsvTableEditorSwing editor && editor.isReordering()) {
            updateTableImmediately();
            return;
        }

        updateScheduler.cancel();
        updateScheduler.schedule(this::updateTableImmediately, UPDATE_DELAY_MILLIS);
    }

    void updateTableImmediately() {
        if (isDisposed() || isSuspended()) return;
        if (updateScheduler != null) updateScheduler.cancel();

        getPsiFileHolder().beforeTableModelUpdate();
        try {
            ModelState state = preparedState;
            preparedState = null;
            if (state == null || !applyState(state)) {
                headerTexts = null;
                super.notifyUpdate();
            } else {
                headerTexts = state.headers();
            }
            fireTableChanged(new TableModelEvent(this));
        } finally {
            getPsiFileHolder().afterTableModelUpdate();
        }
    }

    void prepareUpdate(ModelState state) {
        preparedState = state;
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
        if (!getPsiFileHolder().isEditable()) return;
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
        if (headerTexts == null) {
            headerTexts = readHeaderTexts(getPsiFile());
        }
        return column < headerTexts.size() ? headerTexts.get(column) : "";
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
