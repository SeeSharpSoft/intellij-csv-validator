package net.seesharpsoft.intellij.plugins.csv.components;

import com.intellij.openapi.components.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@State(
        name = "CsvFileAttributes",
        storages = {@Storage(CsvFileAttributes.CSV_STATE_STORAGE_FILE)}
)
public class CsvFileAttributes implements PersistentStateComponent<CsvFileAttributes> {

    public static final String CSV_STATE_STORAGE_FILE = "csv-plugin.xml";

    static class Attribute {
        public String separator;
    }

    public Map<String, Attribute> attributeMap = new HashMap<>();

    @Nullable
    @Override
    public CsvFileAttributes getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull CsvFileAttributes state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    @Override
    public void noStateLoaded() { }

    protected String generateMapKey(@NotNull VirtualFile virtualFile) {
        return virtualFile.getPresentableUrl();
    }

    public void setFileSeparator(@NotNull VirtualFile virtualFile, @NotNull String separator) {
        Attribute state = attributeMap.get(generateMapKey(virtualFile));
        if (state == null) {
            state = new Attribute();
            attributeMap.put(generateMapKey(virtualFile), state);
        }
        state.separator = separator;
    }

    public void removeFileSeparator(@NotNull VirtualFile virtualFile) {
        attributeMap.remove(generateMapKey(virtualFile));
    }

    public String getFileSeparator(@NotNull VirtualFile virtualFile) {
        Attribute state = attributeMap.get(generateMapKey(virtualFile));
        if (state != null) {
            return state.separator;
        }
        return null;
    }
}
