package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.lang.*;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.TokenType;
import com.intellij.psi.impl.source.DummyHolder;
import com.intellij.psi.impl.source.DummyHolderFactory;
import com.intellij.psi.impl.source.tree.FileElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvField;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvRecord;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;

import java.util.HashMap;
import java.util.Map;

public final class CsvHelper {

    private CsvHelper() {
        // static utility class
    }

    // replaces PsiElementFactory.SERVICE.getInstance(element.getProject()).createDummyHolder("<undefined>", CsvTypes.FIELD, null);
    // https://github.com/SeeSharpSoft/intellij-csv-validator/issues/4
    public static PsiElement createEmptyCsvField(Project project) {
        final String text = "<undefined>";
        final IElementType type = CsvTypes.FIELD;
        final PsiManager psiManager = PsiManager.getInstance(project);
        final DummyHolder dummyHolder = DummyHolderFactory.createHolder(psiManager, null);
        final FileElement fileElement = dummyHolder.getTreeElement();
        final ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(CsvLanguage.INSTANCE);
        final Lexer lexer = parserDefinition.createLexer(project);
        final PsiBuilder psiBuilder = PsiBuilderFactory.getInstance().createBuilder(project, fileElement, lexer, CsvLanguage.INSTANCE, text);
        final ASTNode node = parserDefinition.createParser(project).parse(type, psiBuilder);
        fileElement.rawAddChildren((com.intellij.psi.impl.source.tree.TreeElement) node);
        return node.getPsi();
    }

    public static IElementType getElementType(PsiElement element) {
        return element == null || element.getNode() == null ? null : element.getNode().getElementType();
    }

    public static PsiElement getParentFieldElement(PsiElement element) {
        IElementType elementType = CsvHelper.getElementType(element);

        if (elementType == CsvTypes.COMMA || elementType == CsvTypes.CRLF) {
            element = element.getPrevSibling();
            elementType = CsvHelper.getElementType(element);
        }

        if (elementType == CsvTypes.RECORD) {
            element = element.getLastChild();
            elementType = CsvHelper.getElementType(element);
        }

        if (elementType == TokenType.WHITE_SPACE) {
            if (CsvHelper.getElementType(element.getParent()) == CsvTypes.FIELD) {
                element = element.getParent();
            } else if (CsvHelper.getElementType(element.getPrevSibling()) == CsvTypes.FIELD) {
                element = element.getPrevSibling();
            } else if (CsvHelper.getElementType(element.getNextSibling()) == CsvTypes.FIELD) {
                element = element.getNextSibling();
            } else {
                element = null;
            }
        } else {
            while (element != null && elementType != CsvTypes.FIELD) {
                element = element.getParent();
                elementType = CsvHelper.getElementType(element);
            }
        }
        return element;
    }

    public static PsiElement getPreviousCRLF(PsiElement recordElement) {
        while (recordElement != null) {
            if (CsvHelper.getElementType(recordElement) == CsvTypes.CRLF) {
                break;
            }
            recordElement = recordElement.getPrevSibling();
        }
        return recordElement;
    }

    public static PsiElement getNextCRLF(PsiElement recordElement) {
        while (recordElement != null) {
            if (CsvHelper.getElementType(recordElement) == CsvTypes.CRLF) {
                break;
            }
            recordElement = recordElement.getNextSibling();
        }
        return recordElement;
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

    public static CsvColumnInfoMap<PsiElement> createColumnInfoMap(CsvFile csvFile) {
        Map<Integer, CsvColumnInfo<PsiElement>> columnInfoMap = new HashMap<>();
        CsvRecord[] records = PsiTreeUtil.getChildrenOfType(csvFile, CsvRecord.class);
        int row = 0;
        for (CsvRecord record : records) {
            int column = 0;
            for (CsvField field : record.getFieldList()) {
                Integer length = field.getTextLength();
                if (!columnInfoMap.containsKey(column)) {
                    columnInfoMap.put(column, new CsvColumnInfo(column, length));
                } else if (columnInfoMap.get(column).getMaxLength() < length) {
                    columnInfoMap.get(column).setMaxLength(length);
                }
                columnInfoMap.get(column).addElement(field, row, getFieldStartOffset(field), getFieldEndOffset(field));
                ++column;
            }
            ++row;
        }
        return new CsvColumnInfoMap(columnInfoMap);
    }
}
