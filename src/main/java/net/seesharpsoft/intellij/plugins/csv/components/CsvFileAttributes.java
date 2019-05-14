package net.seesharpsoft.intellij.plugins.csv.components;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.xmlb.XmlSerializerUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvStorageHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@State(
        name = "CsvFileAttributes",
        storages = {@Storage(CsvStorageHelper.CSV_STATE_STORAGE_FILE)}
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
        return generateMapKey(psiFile.getProject(), psiFile.getOriginalFile().getVirtualFile());
    }

    protected String generateMapKey(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return CsvStorageHelper.getRelativeFileUrl(project, virtualFile);
    }

    public void setFileSeparator(@NotNull PsiFile psiFile, @NotNull String separator) {
        String key = generateMapKey(psiFile);
        if (key == null) {
            return;
        }
        Attribute state = attributeMap.get(key);
        if (state == null) {
            state = new Attribute();
            attributeMap.put(key, state);
        }
        state.separator = separator;
    }

    public void removeFileSeparator(@NotNull PsiFile psiFile) {
        String key = generateMapKey(psiFile);
        if (key != null) {
            attributeMap.remove(key);
        }
    }

    public String getFileSeparator(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        String key = generateMapKey(project, virtualFile);
        if (key != null) {
            Attribute state = attributeMap.get(key);
            if (state != null) {
                return state.separator;
            }
        }
        return null;
    }

    public String getFileSeparator(@NotNull PsiFile psiFile) {
        return getFileSeparator(psiFile.getProject(), psiFile.getOriginalFile().getVirtualFile());
    }
}
