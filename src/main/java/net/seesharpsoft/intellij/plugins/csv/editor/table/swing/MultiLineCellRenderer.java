package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.UserDataHolder;
import net.seesharpsoft.intellij.plugins.csv.editor.CsvEditorSettingsExternalizable;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvColorSettings;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.util.*;

public class MultiLineCellRenderer extends JTextArea implements TableCellRenderer, TableCellEditor {

    private Set<CellEditorListener> cellEditorListenerSet = new HashSet<>();
    private final UserDataHolder userDataHolder;

    public MultiLineCellRenderer(CsvTableEditorKeyListener keyListener, UserDataHolder userDataHolderParam) {
        setLineWrap(true);
        setWrapStyleWord(true);
        setOpaque(true);
        addKeyListener(keyListener);
        this.userDataHolder = userDataHolderParam;
    }

    private TextAttributes getColumnTextAttributes(int column) {
        if (CsvEditorSettingsExternalizable.getInstance().isTableColumnHighlightingEnabled()) {
            return CsvColorSettings.getTextAttributesOfColumn(column, userDataHolder);
        }
        return null;
    }

    private Color getColumnForegroundColor(int column, Color fallback) {
        TextAttributes textAttributes = getColumnTextAttributes(column);
        return textAttributes == null || textAttributes.getForegroundColor() == null ? fallback : textAttributes.getForegroundColor();
    }

    private Color getColumnBackgroundColor(int column, Color fallback) {
        TextAttributes textAttributes = getColumnTextAttributes(column);
        return textAttributes == null || textAttributes.getBackgroundColor() == null ? fallback : textAttributes.getBackgroundColor();
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (isSelected) {
            setForeground(table.getSelectionForeground());
            setBackground(table.getSelectionBackground());
        } else {
            setForeground(getColumnForegroundColor(column, table.getForeground()));
            setBackground(getColumnBackgroundColor(column, table.getBackground()));
        }
        setFont(table.getFont());
        if (hasFocus) {
            setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
            if (table.isCellEditable(row, column)) {
                setForeground(UIManager.getColor("Table.focusCellForeground"));
                setBackground(UIManager.getColor("Table.focusCellBackground"));
            }
        } else {
            setBorder(new EmptyBorder(1, 2, 1, 2));
        }
        setText((value == null) ? "" : value.toString());

        final int columnWidth = table.getColumnModel().getColumn(column).getWidth();
        final int rowHeight = table.getRowHeight(row);
        this.setSize(columnWidth, rowHeight);
        this.validate();

        return this;
    }

    @Override
    public Dimension getPreferredSize() {
        try {
            final Rectangle rectangle = this.modelToView(getDocument().getLength());
            if (rectangle != null) {
                return new Dimension(this.getWidth(),
                        this.getInsets().top + rectangle.y + rectangle.height +
                                this.getInsets().bottom);
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        return super.getPreferredSize();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        return getTableCellRendererComponent(table, value, isSelected, true, row, column);
    }

    @Override
    public Object getCellEditorValue() {
        return this.getText();
    }

    @Override
    public boolean isCellEditable(EventObject anEvent) {
        return true;
    }

    @Override
    public boolean shouldSelectCell(EventObject anEvent) {
        return true;
    }

    @Override
    public boolean stopCellEditing() {
        fireStopCellEditing();
        return true;
    }

    @Override
    public void cancelCellEditing() {
        fireCancelCellEditing();
    }

    protected void fireStopCellEditing() {
        ChangeEvent changeEvent = new ChangeEvent(this);
        synchronized (cellEditorListenerSet) {
           Iterator<CellEditorListener> it = cellEditorListenerSet.iterator();
           while (it.hasNext()) {
               it.next().editingStopped(changeEvent);
           }
       }
    }

    protected void fireCancelCellEditing() {
        ChangeEvent changeEvent = new ChangeEvent(this);
        synchronized (cellEditorListenerSet) {
            Iterator<CellEditorListener> it = cellEditorListenerSet.iterator();
            while (it.hasNext()) {
                it.next().editingCanceled(changeEvent);
            }
        }
    }

    @Override
    public void addCellEditorListener(CellEditorListener cellEditorListener) {
        synchronized (cellEditorListenerSet) {
            cellEditorListenerSet.add(cellEditorListener);
        }
    }

    @Override
    public void removeCellEditorListener(CellEditorListener cellEditorListener) {
        synchronized (cellEditorListenerSet) {
            cellEditorListenerSet.remove(cellEditorListener);
        }
    }
}
