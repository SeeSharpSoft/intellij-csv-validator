package net.seesharpsoft.intellij.plugins.csv;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CsvColumnInfoMap<T> {

    private final Map<Integer, CsvColumnInfo<T>> infoColumnMap;
    private final Map<T, CsvColumnInfo<T>> reverseInfoColumnMap;

    public CsvColumnInfoMap(Map<Integer, CsvColumnInfo<T>> infoColumnMap) {
        this.infoColumnMap = infoColumnMap;
        this.reverseInfoColumnMap = new HashMap<>();
        buildReverseMap();
    }

    private void buildReverseMap() {
        for (CsvColumnInfo<T> columnInfo : this.infoColumnMap.values()) {
            for (T element : columnInfo.getElements()) {
                this.reverseInfoColumnMap.put(element, columnInfo);
            }
        }
    }

    public CsvColumnInfo<T> getColumnInfo(T element) {
        return reverseInfoColumnMap.get(element);
    }

    public CsvColumnInfo<T> getColumnInfo(int columnIndex) {
        return infoColumnMap.get(columnIndex);
    }

    public Map<Integer, CsvColumnInfo<T>> getColumnInfos() {
        return Collections.unmodifiableMap(this.infoColumnMap);
    }
}
