package net.seesharpsoft.intellij.ui;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

public class CustomDisplayListCellRenderer<E> implements ListCellRenderer<E> {
    private final ListCellRenderer myDelegate;
    private final Function<E, String> myDisplayFunction;

    public CustomDisplayListCellRenderer(Function<E, String> displayFunction) {
        this(new DefaultListCellRenderer(), displayFunction);
    }

    public CustomDisplayListCellRenderer(ListCellRenderer delegate, Function<E, String> displayFunction) {
        this.myDelegate = delegate;
        this.myDisplayFunction = displayFunction;
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends E> list, E value, int index, boolean isSelected, boolean cellHasFocus) {
        String displayValue = myDisplayFunction.apply(value);

        return myDelegate.getListCellRendererComponent(list, displayValue, index, isSelected, cellHasFocus);
    }
}
