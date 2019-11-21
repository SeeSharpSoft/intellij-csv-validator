package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import org.mockito.Mockito;

import javax.swing.*;
import java.awt.event.MouseWheelEvent;

public class CsvTableEditorMouseWheelListenerTest extends CsvTableEditorSwingTestBase {

    @Override
    protected String getTestFile() { return "AnyFile.csv"; }

    public void testZoomOnCsvTable() {
        CsvTableEditorSwing fileEditor = Mockito.spy(this.fileEditor);

        MouseWheelEvent wheelEvent = new MouseWheelEvent(fileEditor.getTable(),
                MouseWheelEvent.MOUSE_WHEEL,JComponent.WHEN_FOCUSED,
                MouseWheelEvent.CTRL_MASK,0,0,0,false,MouseWheelEvent.WHEEL_UNIT_SCROLL,
        3,-1);

        CsvTableEditorMouseWheelListener spiedMouseWheelListener = fileEditor.tableEditorMouseWheelListener;

        int size = fileEditor.getTable().getFont().getSize();
        spiedMouseWheelListener.mouseWheelMoved(wheelEvent);
        int new_size = fileEditor.getTable().getFont().getSize();

        assertEquals(size + 1, new_size);
    }

    public void testScrollOnCsvTable() {
        CsvTableEditorSwing fileEditor = Mockito.spy(this.fileEditor);

        MouseWheelEvent wheelEvent = new MouseWheelEvent(fileEditor.getTable(),
                MouseWheelEvent.MOUSE_WHEEL,JComponent.WHEN_FOCUSED,
                0,0,0,0,false, MouseWheelEvent.WHEEL_UNIT_SCROLL,
                1,1);

        CsvTableEditorMouseWheelListener spiedMouseWheelListener = fileEditor.tableEditorMouseWheelListener;

        int scrollValue = fileEditor.getTableScrollPane().getVerticalScrollBar().getValue();
        assertEquals(0, scrollValue);
        spiedMouseWheelListener.mouseWheelMoved(wheelEvent);
        scrollValue = fileEditor.getTableScrollPane().getVerticalScrollBar().getValue();

        // 90 is max in this case
        assertEquals(90, scrollValue);
    }
}
