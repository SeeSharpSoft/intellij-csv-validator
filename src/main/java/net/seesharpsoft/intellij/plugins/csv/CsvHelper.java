package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageUtil;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import net.seesharpsoft.intellij.plugins.csv.components.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.components.CsvFileAttributes;
import net.seesharpsoft.intellij.plugins.csv.components.CsvValueSeparator;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvField;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvRecord;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.psi.PsiHelper;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CsvHelper {

    public static boolean isCsvFile(FileType fileType) {
        Language language = LanguageUtil.getFileTypeLanguage(fileType);
        return language != null && language.isKindOf(CsvLanguage.INSTANCE);
    }

    public static boolean isCsvFile(String extension) {
        return extension != null && isCsvFile(FileTypeRegistry.getInstance().getFileTypeByExtension(extension));
    }

    public static boolean isCsvFile(VirtualFile file) {
        return file != null && isCsvFile(file.getFileType());
    }

    public static boolean isCsvFile(PsiFile file) {
        return file != null && isCsvFile(getVirtualFile(file));
    }

    public static boolean isCommentElement(PsiElement element) {
        return PsiHelper.getElementType(element) == CsvTypes.COMMENT;
    }

    public static CsvField getParentFieldElement(final PsiElement element) {
        PsiElement currentElement = element;
        IElementType elementType = PsiHelper.getElementType(currentElement);

        if (elementType == CsvTypes.COMMA || elementType == CsvTypes.CRLF) {
            currentElement = currentElement.getPrevSibling();
            elementType = PsiHelper.getElementType(currentElement);
        }

        if (elementType == CsvTypes.RECORD) {
            currentElement = currentElement.getLastChild();
            elementType = PsiHelper.getElementType(currentElement);
        }

        if (elementType == TokenType.WHITE_SPACE) {
            if (PsiHelper.getElementType(currentElement.getParent()) == CsvTypes.FIELD) {
                currentElement = currentElement.getParent();
            } else if (PsiHelper.getElementType(currentElement.getPrevSibling()) == CsvTypes.FIELD) {
                currentElement = currentElement.getPrevSibling();
            } else if (PsiHelper.getElementType(currentElement.getNextSibling()) == CsvTypes.FIELD) {
                currentElement = currentElement.getNextSibling();
            } else {
                currentElement = null;
            }
        } else {
            while (currentElement != null && elementType != CsvTypes.FIELD) {
                currentElement = currentElement.getParent();
                elementType = PsiHelper.getElementType(currentElement);
            }
        }
        return (CsvField) currentElement;
    }

    public static int getFieldIndex(@NotNull final PsiElement element) {
        CsvField fieldElement = getParentFieldElement(element);
        return PsiHelper.getChildIndexOfType(fieldElement.getParent(), fieldElement, CsvField.class);
    }

    public static VirtualFile getVirtualFile(PsiFile psiFile) {
        if (psiFile == null) return null;
        PsiFile original = psiFile.getOriginalFile();
        if (original != psiFile) return getVirtualFile(psiFile.getOriginalFile());
        if (psiFile.getVirtualFile() != null) return psiFile.getVirtualFile();
        return psiFile.getViewProvider().getVirtualFile();
    }

    public static Project getProject(PsiFile psiFile) {
        return psiFile == null ? null : psiFile.getProject();
    }

    public static @NotNull CsvValueSeparator getValueSeparator(CsvFile csvFile) {
        return getValueSeparator(csvFile.getContainingFile());
    }

    public static @NotNull CsvValueSeparator getValueSeparator(PsiFile psiFile) {
        return getValueSeparator(getProject(psiFile), getVirtualFile(psiFile));
    }

    public static @NotNull CsvValueSeparator getValueSeparator(Project project, VirtualFile virtualFile) {
        return project == null ?
                CsvEditorSettings.getInstance().getDefaultValueSeparator() :
                CsvFileAttributes.getInstance(project).getValueSeparator(project, virtualFile);
    }

    public static boolean hasValueSeparatorAttribute(@NotNull PsiFile psiFile) {
        return CsvFileAttributes.getInstance(getProject(psiFile)).hasValueSeparatorAttribute(getProject(psiFile), getVirtualFile(psiFile));
    }

    public static @NotNull CsvEscapeCharacter getEscapeCharacter(CsvFile csvFile) {
        return getEscapeCharacter(csvFile.getContainingFile());
    }

    public static @NotNull CsvEscapeCharacter getEscapeCharacter(PsiFile psiFile) {
        return getEscapeCharacter(getProject(psiFile), getVirtualFile(psiFile));
    }

    public static @NotNull CsvEscapeCharacter getEscapeCharacter(Project project, VirtualFile virtualFile) {
        return project == null ?
                CsvEditorSettings.getInstance().getDefaultEscapeCharacter() :
                CsvFileAttributes.getInstance(project).getEscapeCharacter(project, virtualFile);
    }

    public static boolean hasEscapeCharacterAttribute(@NotNull PsiFile psiFile) {
        return CsvFileAttributes.getInstance(getProject(psiFile)).hasEscapeCharacterAttribute(getProject(psiFile), getVirtualFile(psiFile));
    }

    public static int getColumnCount(PsiFile csvFile) {
        int columns = 0;
        for (PsiElement child = csvFile == null ? null : csvFile.getFirstChild();
             child != null; child = child.getNextSibling()) {
            if (child instanceof CsvRecord) {
                columns = Math.max(columns, PsiTreeUtil.countChildrenOfType(child, CsvField.class));
            }
        }
        return columns;
    }

    public static String getFieldValue(PsiElement field, CsvEscapeCharacter escapeCharacter) {
        if (field == null) return "";
        if (isCommentElement(field)) {
            return field.getText().substring(CsvEditorSettings.getInstance().getCommentIndicator().length());
        }
        return PsiHelper.findChildrenOfAnyElement(field, CsvTypes.TEXT, CsvTypes.ESCAPED_TEXT)
                .stream()
                .map(element -> {
                    String text = element.getText();
                    return PsiHelper.getElementType(element) == CsvTypes.ESCAPED_TEXT && escapeCharacter.isEscapedQuote(text) ? "\"" : text;
                })
                .reduce(String::concat)
                .orElse("");
    }

    public static String unquoteCsvValue(String content, CsvEscapeCharacter escapeCharacter) {
        if (content == null) {
            return "";
        }
        String result = content;
        String trimmedContent = content.trim();
        if (trimmedContent.length() > 1 && trimmedContent.startsWith("\"") && trimmedContent.endsWith("\"")) {
            result = trimmedContent.substring(1, trimmedContent.length() - 1);
            if (escapeCharacter != null) {
                result = result.replaceAll("(?:" + escapeCharacter.getStringPattern() + ")\"", "\"");
            }
        }
        return result;
    }

    private static boolean isQuotingRequired(String content, CsvValueSeparator valueSeparator) {
        return content != null &&
                (content.contains(valueSeparator.getCharacter()) || content.contains("\"") || content.contains("\n"));
    }

    public static String quoteCsvField(String content,
                                       CsvEscapeCharacter escapeCharacter,
                                       CsvValueSeparator valueSeparator,
                                       boolean quotingEnforced) {
        if (content == null) {
            return "";
        }
        if (quotingEnforced || isQuotingRequired(content, valueSeparator)) {
            String result = content;
            result = result.replaceAll("\"", escapeCharacter.getStringPattern() + "\"");
            return "\"" + result + "\"";
        }
        return content;
    }

    public static int getMaxTextLineLength(String text, @NotNull Function<String, Integer> calcCallback) {
        if (text == null) {
            return 0;
        }
        int maxLength = -1;
        for (String line : text.split("(\\r?\\n|\\r)+")) {
            int length = calcCallback.apply(line);
            if (length > maxLength) {
                maxLength = length;
            }
        }
        return maxLength;
    }

    // source: https://www.baeldung.com/java-string-formatting-named-placeholders
    public static String formatString(String template, Map<String, Object> parameters) {
        StringBuilder newTemplate = new StringBuilder(template);
        List<Object> valueList = new ArrayList<>();

        Matcher matcher = Pattern.compile("[$][{](\\w+)}").matcher(template);

        while (matcher.find()) {
            String key = matcher.group(1);

            String paramName = "${" + key + "}";
            int index = newTemplate.indexOf(paramName);
            if (index != -1) {
                newTemplate.replace(index, index + paramName.length(), "%s");
                valueList.add(parameters.get(key));
            }
        }

        return String.format(newTemplate.toString(), valueList.toArray());
    }

    private CsvHelper() {
        // static utility class
    }
}
