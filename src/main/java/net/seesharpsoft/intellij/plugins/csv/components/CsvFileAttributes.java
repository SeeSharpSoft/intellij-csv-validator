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
import com.intellij.util.xmlb.annotations.OptionTag;
import net.seesharpsoft.intellij.plugins.csv.*;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
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
        // holds the actual separator character
        @OptionTag(converter = CsvValueSeparator.CsvValueSeparatorConverter.class)
        public CsvValueSeparator separator;
        public CsvEscapeCharacter escapeCharacter;
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

    public void setFileSeparator(@NotNull PsiFile psiFile, @NotNull CsvValueSeparator separator) {
        if (!canChangeValueSeparator(psiFile)) {
            return;
        }
        Attribute attribute = getFileAttribute(psiFile.getProject(), psiFile.getOriginalFile().getVirtualFile(), true);
        attribute.separator = separator;
    }

    public void resetValueSeparator(@NotNull PsiFile psiFile) {
        if (!canChangeValueSeparator(psiFile)) {
            return;
        }
        Attribute attribute = getFileAttribute(psiFile.getProject(), psiFile.getOriginalFile().getVirtualFile());
        if (attribute != null) {
            attribute.separator = null;
        }
    }

    public @NotNull
    CsvValueSeparator getValueSeparator(Project project, VirtualFile virtualFile) {
        if (project == null || virtualFile == null || !(virtualFile.getFileType() instanceof LanguageFileType)) {
            return CsvEditorSettings.getInstance().getDefaultValueSeparator();
        }
        Language language = ((LanguageFileType) virtualFile.getFileType()).getLanguage();
        if (language instanceof CsvSeparatorHolder) {
                return ((CsvSeparatorHolder) language).getSeparator();
        }
        Attribute attribute = getFileAttribute(project, virtualFile);
        return attribute == null || attribute.separator == null ?
                CsvEditorSettings.getInstance().getDefaultValueSeparator() :
                attribute.separator;
    }

    public boolean hasValueSeparatorAttribute(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        Attribute attribute = getFileAttribute(project, virtualFile);
        return attribute != null && attribute.separator != null;
    }

    public void setEscapeCharacter(@NotNull PsiFile psiFile, @NotNull CsvEscapeCharacter escapeCharacter) {
        Attribute attribute = getFileAttribute(psiFile.getProject(), psiFile.getOriginalFile().getVirtualFile(), true);
        attribute.escapeCharacter = escapeCharacter;
    }

    public void resetEscapeSeparator(@NotNull PsiFile psiFile) {
        Attribute attribute = getFileAttribute(psiFile.getProject(), psiFile.getOriginalFile().getVirtualFile());
        if (attribute != null) {
            attribute.escapeCharacter = null;
        }
    }

    public @NotNull
    CsvEscapeCharacter getEscapeCharacter(Project project, VirtualFile virtualFile) {
        if (project == null || virtualFile == null) {
            return CsvEditorSettings.getInstance().getDefaultEscapeCharacter();
        }
        Attribute attribute = getFileAttribute(project, virtualFile);
        return attribute == null || attribute.escapeCharacter == null ?
                CsvEditorSettings.getInstance().getDefaultEscapeCharacter() :
                attribute.escapeCharacter;
    }

    public boolean hasEscapeCharacterAttribute(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        Attribute attribute = getFileAttribute(project, virtualFile);
        return attribute != null && attribute.escapeCharacter != null;
    }
}
