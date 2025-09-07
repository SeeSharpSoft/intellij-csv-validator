package net.seesharpsoft.intellij.plugins.csv.components;

import com.intellij.lang.Language;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.OptionTag;
import net.seesharpsoft.commons.collection.Pair;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import net.seesharpsoft.intellij.plugins.csv.CsvStorageHelper;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@State(
        name = "CsvFileAttributes",
        storages = {@Storage(CsvStorageHelper.CSV_STATE_STORAGE_FILE)}
)
public class CsvFileAttributes implements PersistentStateComponent<CsvFileAttributes> {
    private final static Logger LOG = Logger.getInstance(CsvFileAttributes.class);

    public Map<String, Attribute> attributeMap = new ConcurrentHashMap<>();

    public static class Attribute {
        @OptionTag(converter = CsvValueSeparator.CsvValueSeparatorConverter.class)
        public CsvValueSeparator separator;
        @OptionTag(converter = CsvEscapeCharacter.CsvEscapeCharacterConverter.class)
        public CsvEscapeCharacter escapeCharacter;
    }

    public static CsvFileAttributes getInstance(Project project) {
        CsvFileAttributes csvFileAttributes = project != null ? project.getService(CsvFileAttributes.class) : null;
        return csvFileAttributes == null ? new CsvFileAttributes() : csvFileAttributes;
    }

    @Nullable
    @Override
    public CsvFileAttributes getState() {
        return this;
    }

    @Override
    public synchronized void loadState(@NotNull CsvFileAttributes state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public synchronized void cleanupAttributeMap(@NotNull Project project) {
        List<String> faultyFiles = new ArrayList<>();
        attributeMap.forEach((fileName, attribute) -> {
            if (!CsvStorageHelper.csvFileExists(project, fileName)) {
                LOG.debug(fileName + " not found or not CSV file");
                faultyFiles.add(fileName);
            }
        });
        faultyFiles.forEach(attributeMap::remove);
    }

    public void reset() {
        attributeMap.clear();
    }

    protected String generateMapKey(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return CsvStorageHelper.getRelativeFilePath(project, virtualFile);
    }

    @Nullable
    private synchronized Attribute getFileAttribute(@NotNull Project project, @NotNull VirtualFile virtualFile, boolean createIfMissing) {
        String key = generateMapKey(project, virtualFile);
        if (key == null) {
            return null;
        }
        Attribute attribute = attributeMap.get(key);
        if (attribute == null && createIfMissing) {
            attribute = new Attribute();
            if (!CsvHelper.isCsvFile(virtualFile)) {
                LOG.error("CSV file attribute requested for non CSV file: " + virtualFile.toString());
            } else {
                attributeMap.put(key, attribute);
            }
        }
        return attribute;
    }

    @Nullable
    private Attribute getFileAttribute(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return getFileAttribute(project, virtualFile, false);
    }

    public static boolean canChangeValueSeparator(@Nullable PsiFile psiFile) {
        if (psiFile == null) return false;
        Language language = psiFile.getLanguage();
        return language.isKindOf(CsvLanguage.INSTANCE) && !(psiFile.getFileType() instanceof CsvSeparatorHolder);
    }

    private void setFileSeparator(@NotNull Project project, @NotNull VirtualFile virtualFile, @NotNull CsvValueSeparator separator) {
        Attribute attribute = getFileAttribute(project, virtualFile, true);
        if (attribute != null) {
            attribute.separator = separator;
        }
    }

    public void setFileSeparator(@NotNull PsiFile psiFile, @NotNull CsvValueSeparator separator) {
        if (!canChangeValueSeparator(psiFile)) {
            return;
        }
        setFileSeparator(psiFile.getProject(), psiFile.getOriginalFile().getVirtualFile(), separator);
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

    @NotNull
    private CsvValueSeparator autoDetectOrGetDefaultValueSeparator(Project project, VirtualFile virtualFile) {
        return CsvEditorSettings.getInstance().isAutoDetectValueSeparator() ?
                autoDetectSeparator(project, virtualFile) :
                CsvEditorSettings.getInstance().getDefaultValueSeparator();
    }

    @NotNull
    private CsvValueSeparator autoDetectSeparator(Project project, VirtualFile virtualFile) {
        final Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
        final String text = document == null ? "" : document.getText();
        final List<CsvValueSeparator> applicableValueSeparators = new ArrayList<>(Arrays.asList(CsvValueSeparator.values()));
        final CsvValueSeparator defaultValueSeparator = CsvEditorSettings.getInstance().getDefaultValueSeparator();
        if (defaultValueSeparator.isCustom()) {
            applicableValueSeparators.add(defaultValueSeparator);
        }
        Pair<CsvValueSeparator, Integer> separatorWithCount =
                applicableValueSeparators.parallelStream()
                        // count
                        .map(separator -> {
                            String character = separator.getCharacter();
                            return Pair.of(separator, StringUtils.countMatches(text, character));
                        })
                        // ignore non-matched separators
                        .filter(p -> p.getSecond() > 0)
                        // get the one with most hits
                        .max(Comparator.comparingInt(Pair::getSecond))
                        // failsafe (e.g. empty document)
                        .orElse(null);

        CsvValueSeparator valueSeparator = separatorWithCount != null ?
                separatorWithCount.getFirst() :
                defaultValueSeparator;

        setFileSeparator(project, virtualFile, valueSeparator);
        return valueSeparator;
    }

    @NotNull
    public CsvValueSeparator getValueSeparator(Project project, VirtualFile virtualFile) {
        if (!CsvHelper.isCsvFile(virtualFile)) {
            return CsvEditorSettings.getInstance().getDefaultValueSeparator();
        }
        FileType fileType = virtualFile.getFileType();
        if (fileType instanceof CsvSeparatorHolder) {
            return ((CsvSeparatorHolder) fileType).getSeparator();
        }
        Attribute attribute = getFileAttribute(project, virtualFile);
        return attribute == null || attribute.separator == null || attribute.separator.getCharacter().isEmpty() ?
                autoDetectOrGetDefaultValueSeparator(project, virtualFile) :
                attribute.separator;
    }

    public boolean hasValueSeparatorAttribute(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        Attribute attribute = getFileAttribute(project, virtualFile);
        return attribute != null && attribute.separator != null;
    }

    public void setEscapeCharacter(@NotNull PsiFile psiFile, @NotNull CsvEscapeCharacter escapeCharacter) {
        if (!canChangeValueSeparator(psiFile)) {
            return;
        }
        Attribute attribute = getFileAttribute(psiFile.getProject(), psiFile.getOriginalFile().getVirtualFile(), true);
        if (attribute != null) {
            attribute.escapeCharacter = escapeCharacter;
        }
    }

    public void resetEscapeSeparator(@NotNull PsiFile psiFile) {
        if (!canChangeValueSeparator(psiFile)) {
            return;
        }
        Attribute attribute = getFileAttribute(psiFile.getProject(), psiFile.getOriginalFile().getVirtualFile());
        if (attribute != null) {
            attribute.escapeCharacter = null;
        }
    }

    @NotNull
    public CsvEscapeCharacter getEscapeCharacter(Project project, VirtualFile virtualFile) {
        if (!CsvHelper.isCsvFile(virtualFile)) {
            return CsvEditorSettings.getInstance().getDefaultEscapeCharacter();
        }
        FileType fileType = virtualFile.getFileType();
        if (fileType instanceof CsvEscapeCharacterHolder) {
            return ((CsvEscapeCharacterHolder) fileType).getEscapeCharacter();
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
