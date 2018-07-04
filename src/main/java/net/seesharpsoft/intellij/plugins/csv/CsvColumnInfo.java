package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CsvColumnInfo<T> {

    private int myColumnIndex;
    private int myMaxLength;
    private Map<T, RowInfo> myElementInfos;
    private T myHeaderElement;
    private int mySize;

    public CsvColumnInfo(int columnIndex, int maxLength) {
        this.myColumnIndex = columnIndex;
        this.myMaxLength = maxLength;
        this.myElementInfos = new HashMap<>();
        this.myHeaderElement = null;
        this.mySize = 0;
    }

    public RowInfo getRowInfo(T element) {
        return myElementInfos.get(element);
    }

    public int getColumnIndex() {
        return myColumnIndex;
    }

    public int getMaxLength() {
        return myMaxLength;
    }

    public void setMaxLength(int maxLength) {
        this.myMaxLength = maxLength;
    }

    public int getSize() {
        return this.mySize;
    }

    public List<T> getElements() {
        List<T> result = new ArrayList<>(getSize());
        result.addAll(Collections.nCopies(getSize(), null));
        myElementInfos.values()
                .forEach(rowInfo -> result.set(rowInfo.myRow, rowInfo.myElement));
        return result;
    }

    protected void put(@NotNull T element, @NotNull RowInfo rowInfo) {
        myElementInfos.put(element, rowInfo);
        if (this.getSize() <= rowInfo.myRow) {
            this.mySize = rowInfo.myRow + 1;
        }
        if (rowInfo.myRow == 0) {
            this.myHeaderElement = element;
        }
    }

    public void addElement(T element, int startIndex, int endIndex) {
        this.put(element, new RowInfo(element, myElementInfos.size(), startIndex, endIndex));
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
        return myElementInfos.containsKey(needle);
    }

    public boolean isHeaderElement(@NotNull T element) {
        return element.equals(getHeaderElement());
    }

    public T getHeaderElement() {
        return this.myHeaderElement;
    }

    public class RowInfo {
        private final T myElement;
        private final int myRow;
        private final TextRange myTextRange;

        RowInfo(T element, int row) {
            this(element, row, -1, -1);
        }

        RowInfo(@NotNull T element, @NotNull int row, int startIndex, int endIndex) {
            this.myElement = element;
            this.myRow = row;
            if (startIndex <= endIndex && startIndex >= 0) {
                this.myTextRange = TextRange.create(startIndex, endIndex);
            } else {
                this.myTextRange = null;
            }
        }

        public T getElement() {
            return myElement;
        }

        public int getRowIndex() {
            return myRow;
        }

        public TextRange getTextRange() {
            return myTextRange;
        }

        @Override
        public int hashCode() {
            return myElement.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof CsvColumnInfo.RowInfo)) {
                return false;
            }
            return this.myElement.equals(((RowInfo) other).myElement);
        }
    }
}
