package net.seesharpsoft.intellij.plugins.csv.components;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.psi.PsiFile;
import com.intellij.util.xmlb.XmlSerializerUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@State(
        name = "CsvFileAttributes",
        storages = {@Storage(CsvStorage.CSV_STATE_STORAGE_FILE)}
)
@SuppressWarnings("all")
public class CsvFileAttributes implements PersistentStateComponent<CsvFileAttributes> {

    public Map<String, Attribute> attributeMap = new HashMap<>();

    static class Attribute {
        public String separator;
    }

    @Nullable
    @Override
    public CsvFileAttributes getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull CsvFileAttributes state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    protected String generateMapKey(@NotNull PsiFile psiFile) {
        return CsvStorage.getRelativeFileUrl(psiFile);
    }

    public void setFileSeparator(@NotNull PsiFile psiFile, @NotNull String separator) {
        Attribute state = attributeMap.get(generateMapKey(psiFile));
        if (state == null) {
            state = new Attribute();
            attributeMap.put(generateMapKey(psiFile), state);
        }
        state.separator = separator;
    }

    public void removeFileSeparator(@NotNull PsiFile psiFile) {
        attributeMap.remove(generateMapKey(psiFile));
    }

    public String getFileSeparator(@NotNull PsiFile psiFile) {
        Attribute state = attributeMap.get(generateMapKey(psiFile));
        if (state != null) {
            return state.separator;
        }
        return null;
    }
}
