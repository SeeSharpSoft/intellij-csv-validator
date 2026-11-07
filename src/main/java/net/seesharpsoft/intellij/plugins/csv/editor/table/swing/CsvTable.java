package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import net.seesharpsoft.intellij.plugins.csv.table.ui.CsvTableBase;
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
        paintSnapshot.paint(this, g, !deferPaintWithoutReadAccess(), super::paint);
    }

    @Override
    public void removeNotify() {
        paintSnapshot.clear();
        super.removeNotify();
    }

    @Override
    protected boolean deferPaintWithoutReadAccess() {
        Application application = ApplicationManager.getApplication();
        if (application.isReadAccessAllowed()) return false;
        if (!deferredPaintPending) {
            deferredPaintPending = true;
            var editor = getModel().getPsiFileHolder();
            var project = editor.getProject();
            application.invokeLater(() -> {
                deferredPaintPending = false;
                if (SwingUtilities.getAncestorOfClass(JScrollPane.class, this) instanceof JScrollPane scrollPane &&
                        scrollPane.isShowing()) {
                    // repaint() would lose the callback's read access before rendering.
                    scrollPane.paintImmediately(scrollPane.getVisibleRect());
                }
            }, ModalityState.stateForComponent(this), ignored -> editor.isDisposed() || (project != null && project.isDisposed()));
        }
        return true;
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
