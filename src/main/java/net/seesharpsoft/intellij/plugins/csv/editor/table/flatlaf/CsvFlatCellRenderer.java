package net.seesharpsoft.intellij.plugins.csv.editor.table.flatlaf;

import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.util.ui.JBUI;
import net.seesharpsoft.intellij.plugins.csv.editor.table.swing.CsvMultiLineCellRenderer;
import net.seesharpsoft.intellij.plugins.csv.editor.table.swing.CsvTableEditorKeyListener;
import net.seesharpsoft.intellij.plugins.csv.highlighter.CsvTextAttributeKeys;

import javax.swing.*;
import java.awt.*;

final class CsvFlatCellRenderer extends CsvMultiLineCellRenderer {

    private final boolean comment;

    CsvFlatCellRenderer(CsvTableEditorKeyListener keyListener, UserDataHolder userDataHolder, boolean comment) {
        super(keyListener, userDataHolder);
        this.comment = comment;
    }

    @Override
    protected TextAttributes getColumnTextAttributes(int column) {
        return comment ? CsvTextAttributeKeys.getCommentTextAttributes() : super.getColumnTextAttributes(column);
    }

    int getPreferredUnwrappedWidth() {
        JTextArea textArea = (JTextArea) getViewport().getView();
        boolean lineWrap = textArea.getLineWrap();
        try {
            textArea.setLineWrap(false);
            Insets insets = getInsets();
            return textArea.getPreferredSize().width + insets.left + insets.right;
        } finally {
            textArea.setLineWrap(lineWrap);
        }
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused,
                                                    int row, int column) {
        super.getTableCellRendererComponent(table, value, selected, focused, row, column);
        JComponent textArea = (JComponent) getViewport().getView();
        textArea.setBorder(focused
                ? BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(table.getSelectionBackground()), JBUI.Borders.empty(2, 7))
                : JBUI.Borders.empty(3, 8));
        getViewport().setBackground(textArea.getBackground());
        return this;
    }
}
