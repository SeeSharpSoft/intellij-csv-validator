package net.seesharpsoft.intellij.plugins.csv.components;

import com.intellij.lang.Language;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.xmlb.XmlSerializerUtil;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import net.seesharpsoft.intellij.plugins.csv.CsvSeparatorHolder;
import net.seesharpsoft.intellij.plugins.csv.CsvStorageHelper;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
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
        public CsvEditorSettings.ValueSeparator valueSeparator;
        public CsvEditorSettings.EscapeCharacter escapeCharacter;
    }

    public static CsvFileAttributes getInstance(Project project) {
        CsvFileAttributes csvFileAttributes = project != null ? ServiceManager.getService(project, CsvFileAttributes.class) : null;
        return csvFileAttributes == null ? new CsvFileAttributes() : csvFileAttributes;
    }

    @Nullable
    @Override
    public CsvFileAttributes getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull CsvFileAttributes state) {
        XmlSerializerUtil.copyBean(state, this);
        legacyTransformation();
    }

    @Deprecated
    private void legacyTransformation() {
        for(Map.Entry<String, Attribute> entry : attributeMap.entrySet()) {
            Attribute attribute = entry.getValue();
            if (attribute.valueSeparator == null && attribute.separator != null) {
                attribute.valueSeparator = Arrays.stream(CsvEditorSettings.ValueSeparator.values())
                        .filter(vs -> vs.getCharacter().equals(attribute.separator))
                        .findFirst().orElse(null);
                attribute.separator = null;
            }
        }
    }

    public void reset() {
        attributeMap.clear();
    }

    protected String generateMapKey(@NotNull PsiFile psiFile) {
        return generateMapKey(psiFile.getProject(), psiFile.getOriginalFile().getVirtualFile());
    }

    protected String generateMapKey(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return CsvStorageHelper.getRelativeFileUrl(project, virtualFile);
    }

    @Nullable
    private Attribute getFileAttribute(@NotNull Project project, @NotNull VirtualFile virtualFile, boolean createIfMissing) {
        String key = generateMapKey(project, virtualFile);
        if (key == null) {
            return null;
        }
        Attribute attribute = key != null ? attributeMap.get(key) : null;
        if (attribute == null && createIfMissing) {
            attribute = new Attribute();
            attributeMap.put(key, attribute);
        }
        return attribute;
    }

    @Nullable
    private Attribute getFileAttribute(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return getFileAttribute(project, virtualFile, false);
    }

    public boolean canChangeValueSeparator(@NotNull PsiFile psiFile) {
        Language language = psiFile.getLanguage();
        return language.isKindOf(CsvLanguage.INSTANCE) && !(language instanceof CsvSeparatorHolder);
    }

    public void setFileSeparator(@NotNull PsiFile psiFile, @NotNull CsvEditorSettings.ValueSeparator separator) {
        if (!canChangeValueSeparator(psiFile)) {
            return;
        }
        Attribute attribute = getFileAttribute(psiFile.getProject(), psiFile.getOriginalFile().getVirtualFile(), true);
        attribute.valueSeparator = separator;
    }

    public void resetValueSeparator(@NotNull PsiFile psiFile) {
        if (!canChangeValueSeparator(psiFile)) {
            return;
        }
        Attribute attribute = getFileAttribute(psiFile.getProject(), psiFile.getOriginalFile().getVirtualFile());
        if (attribute != null) {
            attribute.valueSeparator = null;
        }
    }

    public @NotNull CsvEditorSettings.ValueSeparator getFileSeparator(Project project, VirtualFile virtualFile) {
        if (project == null || virtualFile == null) {
            return CsvEditorSettings.getInstance().getDefaultValueSeparator();
        }
        assert(virtualFile.getFileType() instanceof LanguageFileType);
        Language language = ((LanguageFileType) virtualFile.getFileType()).getLanguage();
        if (language instanceof CsvSeparatorHolder) {
                return ((CsvSeparatorHolder) language).getSeparator();
        }
        Attribute attribute = getFileAttribute(project, virtualFile);
        return attribute == null || attribute.valueSeparator == null ?
                CsvEditorSettings.getInstance().getDefaultValueSeparator() :
                attribute.valueSeparator;
    }

    public CsvEditorSettings.ValueSeparator getFileSeparator(@NotNull PsiFile psiFile) {
        return getFileSeparator(psiFile.getProject(), psiFile.getOriginalFile().getVirtualFile());
    }

    public boolean hasSeparatorAttribute(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        Attribute attribute = getFileAttribute(project, virtualFile);
        return attribute != null && attribute.valueSeparator != null;
    }

    public boolean hasSeparatorAttribute(@NotNull PsiFile psiFile) {
        return hasSeparatorAttribute(psiFile.getProject(), psiFile.getOriginalFile().getVirtualFile());
    }

    public void setEscapeCharacter(@NotNull PsiFile psiFile, @NotNull CsvEditorSettings.EscapeCharacter escapeCharacter) {
        Attribute attribute = getFileAttribute(psiFile.getProject(), psiFile.getOriginalFile().getVirtualFile(), true);
        attribute.escapeCharacter = escapeCharacter;
    }

    public void resetEscapeSeparator(@NotNull PsiFile psiFile) {
        Attribute attribute = getFileAttribute(psiFile.getProject(), psiFile.getOriginalFile().getVirtualFile());
        if (attribute != null) {
            attribute.escapeCharacter = null;
        }
    }

    public @NotNull CsvEditorSettings.EscapeCharacter getEscapeCharacter(Project project, VirtualFile virtualFile) {
        if (project == null || virtualFile == null) {
            return CsvEditorSettings.getInstance().getDefaultEscapeCharacter();
        }
        Attribute attribute = getFileAttribute(project, virtualFile);
        return attribute == null || attribute.escapeCharacter == null ?
                CsvEditorSettings.getInstance().getDefaultEscapeCharacter() :
                attribute.escapeCharacter;
    }

    public CsvEditorSettings.EscapeCharacter getEscapeCharacter(@NotNull PsiFile psiFile) {
        return getEscapeCharacter(psiFile.getProject(), psiFile.getOriginalFile().getVirtualFile());
    }

    public boolean hasEscapeCharacterAttribute(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        Attribute attribute = getFileAttribute(project, virtualFile);
        return attribute != null && attribute.escapeCharacter != null;
    }

    public boolean hasEscapeCharacterAttribute(@NotNull PsiFile psiFile) {
        return hasEscapeCharacterAttribute(psiFile.getProject(), psiFile.getOriginalFile().getVirtualFile());
    }
}
