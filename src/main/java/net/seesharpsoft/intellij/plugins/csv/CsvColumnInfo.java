package net.seesharpsoft.intellij.plugins.csv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CsvColumnInfo<T> {

    public CsvColumnInfo(int columnIndex, int maxLength) {
        this.columnIndex = columnIndex;
        this.maxLength = maxLength;
        this.elements = new ArrayList<>();
    }

    public int getColumnIndex() {
        return columnIndex;
    }

    private int columnIndex;

    private int maxLength;

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    private List<T> elements;

    public List<T> getElements() {
        return Collections.unmodifiableList(elements);
    }

    public boolean addElement(T element) {
        return elements.add(element);
    }

    public boolean containsElement(T element) {
        return elements.contains(element);
    }

    public boolean isHeaderElement(T element) {
        return elements.indexOf(element) == 0;
    }

    public T getHeaderElement() {
        return elements.size() > 0 ? elements.get(0) : null;
    }
}
