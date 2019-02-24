package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import net.seesharpsoft.intellij.plugins.csv.editor.table.CsvTableEditor;
import org.mockito.Mockito;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

public class CsvTableEditorMouseWheelListenerTest extends CsvTableEditorSwingTestBase {

    public void testWheeledOnCsvTable() {
        CsvTableEditorSwing fileEditor = Mockito.spy(this.fileEditor);
        
        MouseWheelEvent wheelEvent = new MouseWheelEvent(fileEditor.getTable(),
                MouseWheelEvent.MOUSE_WHEEL,JComponent.WHEN_FOCUSED,
                MouseWheelEvent.CTRL_MASK,0,0,0,false,MouseWheelEvent.WHEEL_UNIT_SCROLL,
        3,-1);

        CsvTableEditorMouseWheelListener spiedMouseWheelListener = fileEditor.tableEditorMouseWheelListener;

        int size=fileEditor.getTable().getFont().getSize();
        System.out.println(size);

        spiedMouseWheelListener.mouseWheelMoved(wheelEvent);

        int new_size=fileEditor.getTable().getFont().getSize();
        System.out.println(new_size);

        assertTrue(new_size == size + 1);
    }
}
