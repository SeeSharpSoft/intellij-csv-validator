package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.Alarm;
import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditor;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvPsiTreeUpdater;
import net.seesharpsoft.intellij.plugins.csv.table.CsvTableModel;
import net.seesharpsoft.intellij.plugins.csv.table.CsvTableModelBase;
import net.seesharpsoft.intellij.plugins.csv.table.CsvTableModelBase.ModelState;
import net.seesharpsoft.intellij.plugins.csv.table.ui.CsvTableModelSwingBase;
import net.seesharpsoft.intellij.util.UpdateScheduler;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.TableModelEvent;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class CsvTableModelSwing extends CsvTableModelSwingBase implements CsvTableModel {

    private final PsiModel dataModel;
    private final UpdateScheduler updateScheduler;
    private ModelState preparedState;
    private List<String> headerTexts;

    private static final int UPDATE_DELAY_MILLIS = 50;

    public CsvTableModelSwing(@NotNull CsvTableEditor psiFileHolder) {
        this(psiFileHolder, createUpdateScheduler(psiFileHolder));
    }

    CsvTableModelSwing(@NotNull CsvTableEditor psiFileHolder, @NotNull UpdateScheduler updateScheduler) {
        this.updateScheduler = updateScheduler;
        this.dataModel = new PsiModel(psiFileHolder);
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
        dataModel.dispose();
        updateScheduler.cancel();
        preparedState = null;
    }

    @Override
    public void notifyUpdate() {
        if (dataModel == null || isDisposed() || isSuspended()) return;

        if (ApplicationManager.getApplication().isDispatchThread() &&
                getPsiFileHolder() instanceof CsvTableEditorSwing editor && editor.isReordering()) {
            updateTableImmediately();
            return;
        }

        updateScheduler.cancel();
        updateScheduler.schedule(this::updateTableImmediately, UPDATE_DELAY_MILLIS);
    }

    @Override
    public void flushPendingUpdates() {
        updateTableImmediately();
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
                dataModel.refreshMetadata();
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

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (!getPsiFileHolder().isEditable()) return;
        setValue(aValue == null ? "" : aValue.toString(), rowIndex, columnIndex);
    }

    @Override
    protected String getHeaderText(int column) {
        if (headerTexts == null) {
            headerTexts = dataModel.headers();
        }
        return column < headerTexts.size() ? headerTexts.get(column) : "";
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return getPsiFileHolder().isEditable();
    }

    @Override
    protected <T> T readModel(Computable<T> computation) {
        return dataModel.read(computation);
    }

    public CsvTableEditor getPsiFileHolder() {
        return dataModel.getPsiFileHolder();
    }

    public boolean isDisposed() {
        return dataModel.isDisposed();
    }

    @Override
    public boolean isSuspended() {
        return dataModel.isSuspended();
    }

    @Override
    public void suspend() {
        dataModel.suspend();
    }

    @Override
    public void resume() {
        dataModel.resume();
    }

    @Override
    public PsiFile getPsiFile() {
        return dataModel.getPsiFile();
    }

    @Override
    public int getRowCount() {
        return dataModel.getRowCount();
    }

    @Override
    public int getColumnCount() {
        return dataModel.getColumnCount();
    }

    @Override
    public boolean isCommentRow(int row) {
        return dataModel.isCommentRow(row);
    }

    @Override
    public boolean hasErrors() {
        return dataModel.hasErrors();
    }

    @Override
    public PsiElement getFieldAt(int row, int column) {
        return dataModel.getFieldAt(row, column);
    }

    @Override
    public @NotNull String getValue(int row, int column) {
        return dataModel.getValue(row, column);
    }

    @Override
    public void setValue(String value, int row, int column) {
        dataModel.setValue(value, row, column);
    }

    public void setValueAt(String value, int row, int column, boolean commitImmediately) {
        dataModel.setValueAt(value, row, column, commitImmediately);
    }

    public void setValuesAt(List<List<String>> values, Set<Integer> comments, int row, int column) {
        dataModel.setValuesAt(values, comments, row, column);
    }

    @Override
    public void addRow(int row, boolean before) {
        dataModel.addRow(row, before);
    }

    @Override
    public void removeRows(Collection<Integer> rows) {
        dataModel.removeRows(rows);
    }

    @Override
    public void addColumn(int column, boolean before) {
        dataModel.addColumn(column, before);
    }

    @Override
    public void removeColumns(Collection<Integer> columns) {
        dataModel.removeColumns(columns);
    }

    @Override
    public void reorderRows(int[] order) {
        dataModel.reorderRows(order);
    }

    @Override
    public void reorderColumns(int[] order) {
        dataModel.reorderColumns(order);
    }

    @Override
    public int getMovableRowCount() {
        return dataModel.getMovableRowCount();
    }

    @Override
    public void clearCells(Collection<Integer> rows, Collection<Integer> columns) {
        dataModel.clearCells(rows, columns);
    }

    public ModelState captureState() {
        return dataModel.captureState();
    }

    protected boolean applyState(ModelState state) {
        return dataModel.applySnapshot(state);
    }

    protected CsvPsiTreeUpdater getPsiTreeUpdater() {
        return dataModel.updater();
    }

    private final class PsiModel extends CsvTableModelBase<CsvTableEditor> {
        PsiModel(CsvTableEditor holder) {
            super(holder);
        }

        @Override
        public void notifyUpdate() {
            CsvTableModelSwing.this.notifyUpdate();
        }

        void refreshMetadata() {
            super.notifyUpdate();
        }

        boolean applySnapshot(ModelState state) {
            return super.applyState(state);
        }

        List<String> headers() {
            return readHeaderTexts(getPsiFile());
        }

        <T> T read(Computable<T> computation) {
            return computeWithReadAccess(computation);
        }

        CsvPsiTreeUpdater updater() {
            return getPsiTreeUpdater();
        }
    }
}
