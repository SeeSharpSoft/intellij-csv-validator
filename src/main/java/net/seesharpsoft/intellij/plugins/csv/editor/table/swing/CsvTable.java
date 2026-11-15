package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import net.seesharpsoft.intellij.plugins.csv.table.CsvTableModelBase;
import net.seesharpsoft.intellij.plugins.csv.table.ui.CsvTableBase;
import net.seesharpsoft.intellij.plugins.csv.table.ui.CsvTableHeader;
import net.seesharpsoft.intellij.plugins.csv.table.ui.CsvTablePaintSnapshot;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.TableModel;

import java.awt.*;

public class CsvTable extends CsvTableBase {
    private boolean deferredPaintPending;
    private final CsvTablePaintSnapshot paintSnapshot = new CsvTablePaintSnapshot();

    public CsvTable(TableModel model) {
        super((CsvTableModelSwing) model);
    }

    @Override
    public CsvTableModelSwing getModel() {
        return (CsvTableModelSwing) super.getModel();
    }

    @Override
    protected boolean usesPaintSnapshot() {
        return true;
    }

    @Override
    public void paint(@NotNull Graphics g) {
        requestPaintSnapshotRefresh();
        paintSnapshot.paint(this, g, false, super::paint);
    }

    public void paintOffscreen(@NotNull Graphics g) {
        CsvTableModelBase.computeWithReadAccess(() -> {
            paintSnapshot.paint(this, g, true, super::paint);
            return null;
        });
    }

    @Override
    public void removeNotify() {
        paintSnapshot.clear();
        super.removeNotify();
    }

    @Override
    protected void requestPaintSnapshotRefresh() {
        Application application = ApplicationManager.getApplication();
        if (!deferredPaintPending) {
            deferredPaintPending = true;
            var editor = getModel().getPsiFileHolder();
            var project = editor.getProject();
            application.invokeLater(() -> {
                try {
                    if (SwingUtilities.getAncestorOfClass(JScrollPane.class, this) instanceof JScrollPane scrollPane &&
                            scrollPane.isShowing()) {
                        refreshPaintSnapshots();
                        // Native painting may reenter an ancestor that prohibits PSI access.
                        scrollPane.paintImmediately(scrollPane.getVisibleRect());
                    }
                } finally {
                    deferredPaintPending = false;
                }
            }, ModalityState.stateForComponent(this), ignored -> editor.isDisposed() || (project != null && project.isDisposed()));
        }
    }

    private void refreshPaintSnapshots() {
        paintSnapshot.refresh(this, super::paint);
        if (getTableHeader() instanceof CsvTableHeader header) {
            header.refreshPaintSnapshot();
        }
    }

    @Override
    protected void paintOverlay(Graphics g) {
        if (getClientProperty(CsvTableRowDragListener.class) instanceof CsvTableRowDragListener drag) {
            drag.paintFeedback(g, this);
        }
    }

    void cancelRowDragging() {
        if (getClientProperty(CsvTableRowDragListener.class) instanceof CsvTableRowDragListener drag) drag.cancelDrag();
    }
}
