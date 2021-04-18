package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.lang.*;
import com.intellij.lexer.Lexer;
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
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

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

    public static boolean isCsvFile(Project project, VirtualFile file) {
        if (project == null || file == null) {
            return false;
        }
        final Language language = LanguageUtil.getLanguageForPsi(project, file);
        return language != null && language.isKindOf(CsvLanguage.INSTANCE);
    }

    public static boolean isCsvFile(PsiFile file) {
        if (file == null) {
            return false;
        }
        return isCsvFile(file.getProject(), file.getOriginalFile().getVirtualFile());
    }

    public static IElementType getElementType(PsiElement element) {
        return element == null || element.getNode() == null ? null : element.getNode().getElementType();
    }

    public static PsiElement getParentFieldElement(final PsiElement element) {
        PsiElement currentElement = element;
        IElementType elementType = CsvHelper.getElementType(currentElement);

        if (elementType == CsvTypes.COMMA || elementType == CsvTypes.CRLF) {
            currentElement = currentElement.getPrevSibling();
            elementType = CsvHelper.getElementType(currentElement);
        }

        if (elementType == CsvTypes.RECORD) {
            currentElement = currentElement.getLastChild();
            elementType = CsvHelper.getElementType(currentElement);
        }

        if (elementType == TokenType.WHITE_SPACE) {
            if (CsvHelper.getElementType(currentElement.getParent()) == CsvTypes.FIELD) {
                currentElement = currentElement.getParent();
            } else if (CsvHelper.getElementType(currentElement.getPrevSibling()) == CsvTypes.FIELD) {
                currentElement = currentElement.getPrevSibling();
            } else if (CsvHelper.getElementType(currentElement.getNextSibling()) == CsvTypes.FIELD) {
                currentElement = currentElement.getNextSibling();
            } else {
                currentElement = null;
            }
        } else {
            while (currentElement != null && elementType != CsvTypes.FIELD) {
                currentElement = currentElement.getParent();
                elementType = CsvHelper.getElementType(currentElement);
            }
        }
        return currentElement;
    }

    public static PsiElement getPreviousCRLF(final PsiElement element) {
        PsiElement currentElement = element;
        while (currentElement != null) {
            if (CsvHelper.getElementType(currentElement) == CsvTypes.CRLF) {
                break;
            }
            currentElement = currentElement.getPrevSibling();
        }
        return currentElement;
    }

    public static PsiElement getNextCRLF(final PsiElement element) {
        PsiElement currentElement = element;
        while (currentElement != null) {
            if (CsvHelper.getElementType(currentElement) == CsvTypes.CRLF) {
                break;
            }
            currentElement = currentElement.getNextSibling();
        }
        return currentElement;
    }

    public static PsiElement getPreviousSeparator(PsiElement fieldElement) {
        PsiElement current = fieldElement;
        while (current != null) {
            if (CsvHelper.getElementType(current) == CsvTypes.COMMA) {
                break;
            }
            current = current.getPrevSibling();
        }
        return current;
    }

    public static PsiElement getNextSeparator(PsiElement fieldElement) {
        PsiElement current = fieldElement;
        while (current != null) {
            if (CsvHelper.getElementType(current) == CsvTypes.COMMA) {
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
        return psiFile == null ? null : psiFile.getOriginalFile().getVirtualFile();
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

    public static CsvColumnInfoMap<PsiElement> createColumnInfoMap(CsvFile csvFile) {
        CsvEscapeCharacter escapeCharacter = getEscapeCharacter(csvFile);
        Map<Integer, CsvColumnInfo<PsiElement>> columnInfoMap = new HashMap<>();
        CsvRecord[] records = PsiTreeUtil.getChildrenOfType(csvFile, CsvRecord.class);
        int row = 0;
        boolean hasComments = false;
        for (CsvRecord record : records) {
            // skip comment records
            if (record.getComment() != null) {
                hasComments = true;
                continue;
            }
            int column = 0;
            for (CsvField field : record.getFieldList()) {
                Integer length = CsvHelper.getMaxTextLineLength(unquoteCsvValue(field.getText(), escapeCharacter));
                if (!columnInfoMap.containsKey(column)) {
                    columnInfoMap.put(column, new CsvColumnInfo(column, length, row));
                } else if (columnInfoMap.get(column).getMaxLength() < length) {
                    columnInfoMap.get(column).setMaxLength(length, row);
                }
                columnInfoMap.get(column).addElement(field, row, getFieldStartOffset(field), getFieldEndOffset(field));
                ++column;
            }
            ++row;
        }
        return new CsvColumnInfoMap(columnInfoMap, PsiTreeUtil.hasErrorElements(csvFile), hasComments);
    }

    public static String unquoteCsvValue(String content, CsvEscapeCharacter escapeCharacter) {
        if (content == null) {
            return "";
        }
        String result = content.trim();
        if (result.length() > 1 && result.startsWith("\"") && result.endsWith("\"")) {
            result = result.substring(1, result.length() - 1);
        }
        if (escapeCharacter != CsvEscapeCharacter.QUOTE) {
            result = result.replaceAll("(?:" + escapeCharacter.getRegexPattern() + ")" +
                    escapeCharacter.getRegexPattern(), escapeCharacter.getRegexPattern());
        }
        result = result.replaceAll("(?:" + escapeCharacter.getRegexPattern() + ")\"", "\"");
        return result;
    }

    private static boolean isQuotingRequired(String content, CsvEscapeCharacter escapeCharacter, CsvValueSeparator valueSeparator) {
        return content != null &&
                (content.contains(valueSeparator.getCharacter()) || content.contains("\"") || content.contains("\n") || content.contains(escapeCharacter.getCharacter()) ||
                        content.startsWith(" ") || content.endsWith(" "));
    }

    public static String quoteCsvField(String content,
                                       CsvEscapeCharacter escapeCharacter,
                                       CsvValueSeparator valueSeparator,
                                       boolean quotingEnforced) {
        if (content == null) {
            return "";
        }
        if (quotingEnforced || isQuotingRequired(content, escapeCharacter, valueSeparator)) {
            String result = content;
            if (escapeCharacter != CsvEscapeCharacter.QUOTE) {
                result = result.replaceAll(escapeCharacter.getRegexPattern(),
                        escapeCharacter.getRegexPattern() + escapeCharacter.getRegexPattern());
            }
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

    public static int getMaxTextLineLength(String text) {
        return getMaxTextLineLength(text, input -> input == null ? 0 : input.length());
    }

    private CsvHelper() {
        // static utility class
    }
}
