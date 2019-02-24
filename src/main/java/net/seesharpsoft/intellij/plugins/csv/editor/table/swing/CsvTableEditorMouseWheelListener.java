package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

public class CsvTableEditorMouseWheelListener extends  CsvTableEditorUtilBase implements MouseWheelListener {

    public static final int SCROLL_FACTOR = 3;

    public CsvTableEditorMouseWheelListener(CsvTableEditorSwing csvTableEditorArg) {
        super(csvTableEditorArg);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent mouseWheelEvent) {

        if (mouseWheelEvent.isControlDown()) {
            int scrolled = mouseWheelEvent.getUnitsToScroll();
            int amount = -(scrolled / SCROLL_FACTOR);
            if (amount==0)
                return;
            csvTableEditor.changeFontSize(amount);
        }
    }
}
