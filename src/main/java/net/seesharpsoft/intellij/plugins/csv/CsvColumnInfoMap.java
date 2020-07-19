package net.seesharpsoft.intellij.plugins.csv;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CsvColumnInfoMap<T> {

    private final Map<Integer, CsvColumnInfo<T>> myInfoColumnMap;
    private final Map<T, CsvColumnInfo<T>> myReverseInfoColumnMap;

    private boolean hasErrors = false;
    private boolean hasComments = false;

    public CsvColumnInfoMap(Map<Integer, CsvColumnInfo<T>> infoColumnMap, boolean hasErrorsArg, boolean hasCommentsArg) {
        this.myInfoColumnMap = infoColumnMap;
        this.myReverseInfoColumnMap = new HashMap<>();
        buildReverseMap();
        setHasErrors(hasErrorsArg);
        setHasComments(hasCommentsArg);
    }

    public CsvColumnInfoMap(Map<Integer, CsvColumnInfo<T>> infoColumnMap) {
        this(infoColumnMap, false, false);
    }

    private void buildReverseMap() {
        for (CsvColumnInfo<T> columnInfo : this.myInfoColumnMap.values()) {
            for (T element : columnInfo.getElements()) {
                this.myReverseInfoColumnMap.put(element, columnInfo);
            }
        }
    }

    public CsvColumnInfo<T> getColumnInfo(T element) {
        return myReverseInfoColumnMap.get(element);
    }

    public CsvColumnInfo<T> getColumnInfo(int columnIndex) {
        return myInfoColumnMap.get(columnIndex);
    }

    public CsvColumnInfo<T>.RowInfo getRowInfo(T element) {
        CsvColumnInfo<T> columnInfo = getColumnInfo(element);
        return columnInfo != null ? columnInfo.getRowInfo(element) : null;
    }

    public Map<Integer, CsvColumnInfo<T>> getColumnInfos() {
        return Collections.unmodifiableMap(this.myInfoColumnMap);
    }

    public boolean hasErrors() {
        return hasErrors;
    }

    public void setHasErrors(boolean hasErrorsArg) {
        hasErrors = hasErrorsArg;
    }

    public boolean hasComments() {
        return hasComments;
    }

    public void setHasComments(boolean hasCommentsArg) {
        hasComments = hasCommentsArg;
    }

    public boolean hasEmptyLastLine() {
        CsvColumnInfo<T> columnInfo = myInfoColumnMap.get(0);
        int size = columnInfo.getSize();
        if (!columnInfo.getRowInfo(size - 1).getTextRange().isEmpty()) {
            return false;
        }
        for (int columnIndex = 1; columnIndex < myInfoColumnMap.size(); ++columnIndex) {
            if (myInfoColumnMap.get(columnIndex).getSize() == size) {
                return false;
            }
        }
        return true;
    }

}
