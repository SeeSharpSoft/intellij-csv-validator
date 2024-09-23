package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.lang.*;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.TokenType;
import com.intellij.psi.impl.source.DummyHolder;
import com.intellij.psi.impl.source.DummyHolderFactory;
import com.intellij.psi.impl.source.tree.FileElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import net.seesharpsoft.intellij.lang.FileParserDefinition;
import net.seesharpsoft.intellij.plugins.csv.components.CsvFileAttributes;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvField;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvRecord;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.plugins.psv.PsvFileType;
import net.seesharpsoft.intellij.plugins.tsv.TsvFileType;
import net.seesharpsoft.intellij.psi.PsiHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CsvHelper {

    // replaces PsiElementFactory.SERVICE.getInstance(element.getProject()).createDummyHolder("<undefined>", CsvTypes.FIELD, null);
    // https://github.com/SeeSharpSoft/intellij-csv-validator/issues/4
    public static PsiElement createEmptyCsvField(PsiFile psiFile) {
        final Project project = psiFile.getProject();
        final String text = "<undefined>";
        final IElementType type = CsvTypes.FIELD;
        final PsiManager psiManager = PsiManager.getInstance(project);
        final DummyHolder dummyHolder = DummyHolderFactory.createHolder(psiManager, null);
        final FileElement fileElement = dummyHolder.getTreeElement();
        final FileParserDefinition parserDefinition = (FileParserDefinition) LanguageParserDefinitions.INSTANCE.forLanguage(CsvLanguage.INSTANCE);
        final Lexer lexer = parserDefinition.createLexer(psiFile);
        final PsiBuilder psiBuilder = PsiBuilderFactory.getInstance().createBuilder(project, fileElement, lexer, CsvLanguage.INSTANCE, text);
        final ASTNode node = parserDefinition.createParser(project).parse(type, psiBuilder);
        fileElement.rawAddChildren((com.intellij.psi.impl.source.tree.TreeElement) node);
        return node.getPsi();
    }

    public static boolean isCsvFile(String extension) {
        if (extension == null) {
            return false;
        }
        // simple check to always in include the defaults even if association was removed
        switch (extension.toLowerCase()) {
            case "csv":
            case "tsv":
            case "tab":
            case "psv":
                return true;
            default:
                // but also consider other extensions that are associated manually
                FileType fileType = FileTypeRegistry.getInstance().getFileTypeByExtension(extension);
                return fileType == CsvFileType.INSTANCE ||
                        fileType == TsvFileType.INSTANCE ||
                        fileType == PsvFileType.INSTANCE;
        }
    }

    public static boolean isCsvFile(Project project, VirtualFile file) {
        if (project == null || file == null || !isCsvFile(file.getExtension())) {
            return false;
        }
        final Language language = LanguageUtil.getLanguageForPsi(project, file);
        return language != null && language.isKindOf(CsvLanguage.INSTANCE);
    }

    public static boolean isCsvFile(PsiFile file) {
        if (file == null) {
            return false;
        }
        return isCsvFile(file.getProject(), getVirtualFile(file));
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

    public static PsiElement getPreviousCRLF(final PsiElement element) {
        PsiElement currentElement = element;
        while (currentElement != null) {
            if (PsiHelper.getElementType(currentElement) == CsvTypes.CRLF) {
                break;
            }
            currentElement = currentElement.getPrevSibling();
        }
        return currentElement;
    }

    public static PsiElement getNextCRLF(final PsiElement element) {
        PsiElement currentElement = element;
        while (currentElement != null) {
            if (PsiHelper.getElementType(currentElement) == CsvTypes.CRLF) {
                break;
            }
            currentElement = currentElement.getNextSibling();
        }
        return currentElement;
    }

    public static PsiElement getPreviousSeparator(PsiElement fieldElement) {
        PsiElement current = fieldElement;
        while (current != null) {
            if (PsiHelper.getElementType(current) == CsvTypes.COMMA) {
                break;
            }
            current = current.getPrevSibling();
        }
        return current;
    }

    public static PsiElement getNextSeparator(PsiElement fieldElement) {
        PsiElement current = fieldElement;
        while (current != null) {
            if (PsiHelper.getElementType(current) == CsvTypes.COMMA) {
                break;
            }
            current = current.getNextSibling();
        }
        return current;
    }

    public static int getFieldStartOffset(PsiElement field) {
        PsiElement separator = CsvHelper.getPreviousSeparator(field);
        if (separator == null) {
            separator = getPreviousCRLF(field.getParent());
        }
        return separator == null ? 0 : separator.getTextOffset() + separator.getTextLength();
    }

    public static int getFieldEndOffset(PsiElement field) {
        PsiElement separator = CsvHelper.getNextSeparator(field);
        if (separator == null) {
            separator = getNextCRLF(field.getParent());
        }
        return separator == null ? field.getContainingFile().getTextLength() : separator.getTextOffset();
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

    public static CsvColumnInfoMap<PsiElement> createColumnInfoMap(PsiFile csvFile) {
        return createColumnInfoMap(csvFile, CsvHelper::getMaxTextLineLength);
    }

    public static CsvColumnInfoMap<PsiElement> createColumnInfoMap(PsiFile csvFile, Function<CsvField, Integer> fnMaxTextLength) {
        Map<Integer, CsvColumnInfo<PsiElement>> columnInfoMap = new HashMap<>();
        int row = 0;
        boolean hasComments = false;
        for (PsiElement child = csvFile.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (!(child instanceof CsvRecord)) {
                if (PsiHelper.getElementType(child) == CsvTypes.COMMENT) {
                    hasComments = true;
                }
                continue;
            }

            CsvRecord record = (CsvRecord) child;
            int column = 0;
            for (CsvField field : record.getFieldList()) {
                int length = fnMaxTextLength.apply(field);
                if (!columnInfoMap.containsKey(column)) {
                    columnInfoMap.put(column, new CsvColumnInfo<>(column, length, row));
                } else if (columnInfoMap.get(column).getMaxLength() < length) {
                    columnInfoMap.get(column).setMaxLength(length, row);
                }
                columnInfoMap.get(column).addElement(field, row, getFieldStartOffset(field), getFieldEndOffset(field));
                ++column;
            }
            ++row;
        }
        return new CsvColumnInfoMap<>(columnInfoMap, PsiTreeUtil.hasErrorElements(csvFile), hasComments);
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

    public static String unquoteCsvValue(String content) {
        return unquoteCsvValue(content, null);
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
                result = result.replaceAll("(?:" + escapeCharacter.getRegexPattern() + ")\"", "\"");
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
            result = result.replaceAll("\"", escapeCharacter.getRegexPattern() + "\"");
            return "\"" + result + "\"";
        }
        return content;
    }

    public static <T> T[][] deepCopy(T[][] matrix) {
        return java.util.Arrays.stream(matrix).map(el -> el.clone()).toArray($ -> matrix.clone());
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

    public static int getMaxTextLineLength(CsvField field) {
        return getMaxTextLineLength(field.getText().strip(), input -> input == null ? 0 : input.length());
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
