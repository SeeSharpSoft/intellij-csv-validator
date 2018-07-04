package net.seesharpsoft.intellij.plugins.csv;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CsvColumnInfoMap<T> {

    private final Map<Integer, CsvColumnInfo<T>> myInfoColumnMap;
    private final Map<T, CsvColumnInfo<T>> myReverseInfoColumnMap;

    public CsvColumnInfoMap(Map<Integer, CsvColumnInfo<T>> infoColumnMap) {
        this.myInfoColumnMap = infoColumnMap;
        this.myReverseInfoColumnMap = new HashMap<>();
        buildReverseMap();
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
}
