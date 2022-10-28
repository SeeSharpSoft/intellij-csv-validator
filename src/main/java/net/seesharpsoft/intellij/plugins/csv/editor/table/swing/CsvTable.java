package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.table.TableModel;
import java.awt.*;

public class CsvTable extends JBTable {

    private int mySuspendCounter = 0;
    private boolean myPaintRequestedWhileSuspended = false;

    public CsvTable(TableModel model) {
        super(model);
    }

    public void suspend() {
        mySuspendCounter++;
    }

    public void resume() {
        mySuspendCounter--;
        if (!isSuspended() && myPaintRequestedWhileSuspended) {
            myPaintRequestedWhileSuspended = false;
            this.repaint();
        }
    }

    public boolean isSuspended() {
        return mySuspendCounter != 0;
    }

    @Override
    public void paint(@NotNull Graphics g) {
        if (isSuspended()) {
            myPaintRequestedWhileSuspended = true;
            return;
        }
        super.paint(g);
    }

}
