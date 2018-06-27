package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CsvColumnInfo<T> {

    public class RowInfo {
        RowInfo(T element, int row) {
            this(element, row, -1, -1);
        }

        RowInfo(@NotNull T element, @NotNull int row, int startIndex, int endIndex) {
            this.element = element;
            this.row = row;
            if (startIndex <= endIndex && startIndex >= 0) {
                this.textRange = TextRange.create(startIndex, endIndex);
            } else {
                this.textRange = null;
            }
        }

        final T element;
        final int row;
        final TextRange textRange;

        public T getElement() {
            return element;
        }

        public int getRowIndex() {
            return row;
        }

        public TextRange getTextRange() {
            return textRange;
        }

        @Override
        public int hashCode() {
            return element.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof CsvColumnInfo.RowInfo)) {
                return false;
            }
            return this.element.equals(((RowInfo) other).element);
        }
    }

    private int columnIndex;
    private int maxLength;
    private Map<T, RowInfo> elementInfos;
    private T headerElement;
    private int size;

    public CsvColumnInfo(int columnIndex, int maxLength) {
        this.columnIndex = columnIndex;
        this.maxLength = maxLength;
        this.elementInfos = new HashMap<>();
        this.headerElement = null;
        this.size = 0;
    }

    public RowInfo getRowInfo(T element) {
        return elementInfos.get(element);
    }

    public int getColumnIndex() {
        return columnIndex;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public int getSize() {
        return this.size;
    }

    public List<T> getElements() {
        List<T> result = new ArrayList<>(getSize());
        result.addAll(Collections.nCopies(getSize(), null));
        elementInfos.values()
                .forEach(rowInfo -> result.set(rowInfo.row, rowInfo.element));
        return result;
    }

    protected void put(@NotNull T element, @NotNull RowInfo rowInfo) {
        RowInfo previous = elementInfos.put(element, rowInfo);
        if (this.getSize() <= rowInfo.row) {
            this.size = rowInfo.row + 1;
        }
        if (rowInfo.row == 0) {
            this.headerElement = element;
        }
    }

    public void addElement(T element, int startIndex, int endIndex) {
        this.put(element, new RowInfo(element, elementInfos.size(), startIndex, endIndex));
    }

    public void addElement(T element) {
        this.addElement(element, -1, -1);
    }

    public void addElement(T element, int row, int startIndex, int endIndex) {
        this.put(element, new RowInfo(element, row, startIndex, endIndex));
    }

    public void addElement(T element, int row) {
        this.addElement(element, row, -1, -1);
    }

    public boolean containsElement(T needle) {
        return elementInfos.containsKey(needle);
    }

    public boolean isHeaderElement(@NotNull T element) {
        return element.equals(getHeaderElement());
    }

    public T getHeaderElement() {
        return this.headerElement;
    }
}
