package net.seesharpsoft.intellij.plugins.csv.editor.table.swing;

import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import net.seesharpsoft.intellij.plugins.csv.highlighter.CsvTextAttributeKeys;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.util.EventObject;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static java.awt.event.InputEvent.CTRL_DOWN_MASK;

public class CsvMultiLineCellRenderer extends JBScrollPane implements TableCellRenderer, TableCellEditor {
    private final Set<CellEditorListener> cellEditorListenerSet = new CopyOnWriteArraySet<>();
    private final UserDataHolder myUserDataHolder;

    private final JTextArea myTextArea;
    private final Border cellPadding = JBUI.Borders.empty(3, 8);

    public CsvMultiLineCellRenderer(CsvTableEditorKeyListener keyListener, UserDataHolder userDataHolderParam) {
        this.myUserDataHolder = userDataHolderParam;
        myTextArea = new JTextArea();
        myTextArea.setLineWrap(true);
        myTextArea.setWrapStyleWord(true);
        myTextArea.setOpaque(true);
        myTextArea.setRequestFocusEnabled(true);
        myTextArea.addKeyListener(keyListener);
        myTextArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, CTRL_DOWN_MASK), myTextArea.getInputMap().get(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)));
        myTextArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "DO_NOTHING_AT_ALL");
        this.setOpaque(true);
        this.setBorder(null);
        this.setViewportView(myTextArea);
    }

    protected TextAttributes getColumnTextAttributes(int column) {
        return CsvTextAttributeKeys.getTextAttributesOfColumn(column, myUserDataHolder);
    }

    @Override
    public boolean contains(int x, int y) {
        if (getParent() instanceof CsvTable table && table.getFrozenColumnCount() > 0 &&
                table.getEditingColumn() >= table.getFrozenColumnCount() && !table.isCommentRow(table.getEditingRow()) &&
                getX() + x < table.getVisibleRect().x + table.getFrozenColumnWidth()) {
            return false;
        }
        return super.contains(x, y);
    }

    private Color getColumnForegroundColor(int column, Color fallback) {
        TextAttributes textAttributes = getColumnTextAttributes(column);
        return textAttributes == null || textAttributes.getForegroundColor() == null ? fallback : textAttributes.getForegroundColor();
    }

    private Color getColumnBackgroundColor(int column, Color fallback) {
        TextAttributes textAttributes = getColumnTextAttributes(column);
        return textAttributes == null || textAttributes.getBackgroundColor() == null ? fallback : textAttributes.getBackgroundColor();
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        // One renderer paints every cell, so neither caret scrolling nor a scroll position may carry over between cells.
        setCaretUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        prepareCell(table, value, isSelected, hasFocus, row, column);
        getViewport().setViewPosition(new Point());
        return this;
    }

    private void prepareCell(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (isSelected) {
            myTextArea.setForeground(table.getSelectionForeground());
            myTextArea.setBackground(table.getSelectionBackground());
        } else {
            myTextArea.setForeground(getColumnForegroundColor(column, table.getForeground()));
            myTextArea.setBackground(getColumnBackgroundColor(column, table.getBackground()));
        }
        if (hasFocus) {
            myTextArea.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(table.getSelectionBackground(), JBUI.scale(1)), JBUI.Borders.empty(2, 7)));
            if (!isSelected && table.isCellEditable(row, column)) {
                myTextArea.setForeground(table.getForeground());
                myTextArea.setBackground(table.getBackground());
            }
        } else {
            myTextArea.setBorder(cellPadding);
        }
        getViewport().setBackground(myTextArea.getBackground());

        this.setFont(table.getFont());
        this.setText(value == null ? "" : value.toString());

        // Wrap at the painted cell's width so scroll bars and line breaks are decided once, at their final size.
        Dimension size = getCellSize(table, row, column);
        this.setSize(size);
        myTextArea.setSize(size);
    }

    protected Dimension getCellSize(JTable table, int row, int column) {
        TableColumnModel columns = table.getColumnModel();
        return new Dimension(columns.getColumn(column).getWidth() - columns.getColumnMargin(),
                table.getRowHeight(row) - table.getRowMargin());
    }

    private void setCaretUpdatePolicy(int policy) {
        if (myTextArea.getCaret() instanceof DefaultCaret caret) caret.setUpdatePolicy(policy);
    }

    int getPreferredUnwrappedWidth() {
        boolean lineWrap = myTextArea.getLineWrap();
        try {
            myTextArea.setLineWrap(false);
            Insets insets = getInsets();
            return myTextArea.getPreferredSize().width + insets.left + insets.right;
        } finally {
            myTextArea.setLineWrap(lineWrap);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        try {
            final Rectangle2D rectangle = myTextArea.modelToView2D(myTextArea.getDocument().getLength());
            if (rectangle != null) {
                return new Dimension(this.getWidth(),
                        (int) (this.getInsets().top + rectangle.getY() + rectangle.getHeight() + this.getInsets().bottom));
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        return super.getPreferredSize();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        // The editor's caret follows typing and keeps itself scrolled into view.
        setCaretUpdatePolicy(DefaultCaret.UPDATE_WHEN_ON_EDT);
        myTextArea.setEditable(table.getModel().isCellEditable(row, column));
        prepareCell(table, value, false, true, row, column);
        return this;
    }

    @Override
    public Object getCellEditorValue() {
        return myTextArea.getText();
    }

    protected void setText(@NotNull String text) {
        Font font = UIUtil.getFontWithFallbackIfNeeded(this.getFont(), text);
        this.setFont(font);
        myTextArea.setFont(font);
        myTextArea.setText(text);
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
            for (CellEditorListener cellEditorListener : cellEditorListenerSet) {
                cellEditorListener.editingStopped(changeEvent);
            }
        }
    }

    protected void fireCancelCellEditing() {
        ChangeEvent changeEvent = new ChangeEvent(this);
        synchronized (cellEditorListenerSet) {
            for (CellEditorListener cellEditorListener : cellEditorListenerSet) {
                cellEditorListener.editingCanceled(changeEvent);
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

    @Override
    protected void processFocusEvent(FocusEvent focusEvent) {
        super.processFocusEvent(focusEvent);
        myTextArea.grabFocus();
    }

    public static class Comment extends CsvMultiLineCellRenderer {
        public Comment(CsvTableEditorKeyListener keyListener, UserDataHolder userDataHolderParam) {
            super(keyListener, userDataHolderParam);
        }

        @Override
        protected TextAttributes getColumnTextAttributes(int column) {
            return CsvTextAttributeKeys.getCommentTextAttributes();
        }

        @Override
        protected Dimension getCellSize(JTable table, int row, int column) {
            // Comments span all columns.
            return table.getCellRect(row, column, false).getSize();
        }
    }
}
