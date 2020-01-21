package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import javax.swing.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

public class CsvTableEditorMouseWheelListener extends CsvTableEditorUtilBase implements MouseWheelListener {

    public static final int ZOOM_FACTOR = 3;

    protected static final int SCROLL_FACTOR = 100;

    public CsvTableEditorMouseWheelListener(CsvTableEditorSwing csvTableEditorArg) {
        super(csvTableEditorArg);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent mouseWheelEvent) {
        if (mouseWheelEvent.isControlDown()) {
            int scrolled = mouseWheelEvent.getUnitsToScroll();
            int amount = -(scrolled / ZOOM_FACTOR);
            if (amount == 0) {
                return;
            }
            csvTableEditor.changeFontSize(amount);
        } else if (mouseWheelEvent.isShiftDown()) {
            JScrollPane scrollPane = csvTableEditor.getTableScrollPane();
            JScrollBar hScrollbar = scrollPane.getHorizontalScrollBar();
            hScrollbar.setValue(hScrollbar.getValue() + (int)(SCROLL_FACTOR * mouseWheelEvent.getPreciseWheelRotation()));
        } else {
            JScrollPane scrollPane = csvTableEditor.getTableScrollPane();
            JScrollBar vScrollbar = scrollPane.getVerticalScrollBar();
            vScrollbar.setValue(vScrollbar.getValue() + (int)(SCROLL_FACTOR * mouseWheelEvent.getPreciseWheelRotation()));
        }
    }
}
