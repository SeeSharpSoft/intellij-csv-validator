package net.seesharpsoft.intellij.plugins.csv.intention;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public final class CsvIntentionHelper {

    private CsvIntentionHelper() {
        // static utility class
    }

    private static final Logger LOG = Logger.getInstance("#net.seesharpsoft.intellij.plugins.csv.inspection.CsvIntentionHelper");

    public static List<PsiElement> getChildren(PsiElement element) {
        List<PsiElement> children = new ArrayList<>();
        if (element != null) {
            element = element.getFirstChild();
            while (element != null) {
                children.add(element);
                element = element.getNextSibling();
            }
        }
        return children;
    }

    public static Collection<PsiElement> getAllElements(PsiFile file) {
        List<PsiElement> todo = getChildren(file);
        Collection<PsiElement> elements = new HashSet();
        while (todo.size() > 0) {
            PsiElement current = todo.get(todo.size() - 1);
            todo.remove(todo.size() - 1);
            elements.add(current);
            todo.addAll(getChildren(current));
        }
        return elements;
    }

    private static Collection<PsiElement> getAllFields(PsiFile file) {
        return getChildren(file).parallelStream()
                .filter(element -> CsvHelper.getElementType(element) == CsvTypes.RECORD)
                .flatMap(record -> getChildren(record).stream())
                .filter(element -> CsvHelper.getElementType(element) == CsvTypes.FIELD)
                .collect(Collectors.toList());
    }

    public static void quoteAll(@NotNull Project project, @NotNull PsiFile psiFile) {
        try {
            Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
            List<Integer> quotePositions = new ArrayList<>();
            Collection<PsiElement> fields = getAllFields(psiFile);
            PsiElement separator;
            for (PsiElement field : fields) {
                if (field.getFirstChild() == null || CsvHelper.getElementType(field.getFirstChild()) != CsvTypes.QUOTE) {
                    separator = CsvHelper.getPreviousSeparator(field);
                    if (separator == null) {
                        quotePositions.add(field.getParent().getTextOffset());
                    } else {
                        quotePositions.add(separator.getTextOffset() + separator.getTextLength());
                    }
                }
                if (field.getLastChild() == null || CsvHelper.getElementType(field.getLastChild()) != CsvTypes.QUOTE) {
                    separator = CsvHelper.getNextSeparator(field);
                    if (separator == null) {
                        quotePositions.add(field.getParent().getTextOffset() + field.getParent().getTextLength());
                    } else {
                        quotePositions.add(separator.getTextOffset());
                    }
                }
            }
            String text = addQuotes(document.getText(), quotePositions);
            document.setText(text);
        } catch (IncorrectOperationException e) {
            LOG.error(e);
        }
    }

    public static void unquoteAll(@NotNull Project project, @NotNull PsiFile psiFile) {
        try {
            Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
            List<Integer> quotePositions = new ArrayList<>();
            Collection<PsiElement> fields = getAllFields(psiFile);
            for (PsiElement field : fields) {
                if (getChildren(field).stream().anyMatch(element -> CsvHelper.getElementType(element) == CsvTypes.ESCAPED_TEXT)) {
                    continue;
                }
                if (CsvHelper.getElementType(field.getFirstChild()) == CsvTypes.QUOTE) {
                    quotePositions.add(field.getFirstChild().getTextOffset());
                }
                if (CsvHelper.getElementType(field.getLastChild()) == CsvTypes.QUOTE) {
                    quotePositions.add(field.getLastChild().getTextOffset());
                }
            }
            String text = removeQuotes(document.getText(), quotePositions);
            document.setText(text);
        } catch (IncorrectOperationException e) {
            LOG.error(e);
        }
    }

    public static void quoteValue(@NotNull Project project, @NotNull PsiElement element) {
        try {
            Document document = PsiDocumentManager.getInstance(project).getDocument(element.getContainingFile());
            List<Integer> quotePositions = new ArrayList<>();

            element = CsvHelper.getParentFieldElement(element);
            int quotePosition = getOpeningQuotePosition(element.getFirstChild(), element.getLastChild());
            if (quotePosition != -1) {
                quotePositions.add(quotePosition);
            }
            PsiElement endSeparatorElement = findQuotePositionsUntilSeparator(element, quotePositions);
            if (endSeparatorElement == null) {
                quotePositions.add(document.getTextLength());
            } else {
                quotePositions.add(endSeparatorElement.getTextOffset());
            }
            String text = addQuotes(document.getText(), quotePositions);
            document.setText(text);
        } catch (IncorrectOperationException e) {
            LOG.error(e);
        }
    }

    public static void unquoteValue(@NotNull Project project, @NotNull PsiElement element) {
        try {
            Document document = PsiDocumentManager.getInstance(project).getDocument(element.getContainingFile());
            List<Integer> quotePositions = new ArrayList<>();

            element = CsvHelper.getParentFieldElement(element);
            if (CsvHelper.getElementType(element.getFirstChild()) == CsvTypes.QUOTE) {
                quotePositions.add(element.getFirstChild().getTextOffset());
            }
            if (CsvHelper.getElementType(element.getLastChild()) == CsvTypes.QUOTE) {
                quotePositions.add(element.getLastChild().getTextOffset());
            }
            String text = removeQuotes(document.getText(), quotePositions);
            document.setText(text);
        } catch (IncorrectOperationException e) {
            LOG.error(e);
        }
    }

    public static String addQuotes(String text, List<Integer> quotePositions) {
        int offset = 0;
        quotePositions.sort(Integer::compareTo);
        for (int position : quotePositions) {
            int offsetPosition = position + offset;
            text = text.substring(0, offsetPosition) + "\"" + text.substring(offsetPosition);
            ++offset;
        }
        return text;
    }

    public static String removeQuotes(String text, List<Integer> quotePositions) {
        int offset = 0;
        quotePositions.sort(Integer::compareTo);
        for (int position : quotePositions) {
            int offsetPosition = position + offset;
            text = text.substring(0, offsetPosition) + text.substring(offsetPosition + 1);
            --offset;
        }
        return text;
    }

    public static int getOpeningQuotePosition(PsiElement firstFieldElement, PsiElement lastFieldElement) {
        if (CsvHelper.getElementType(firstFieldElement) != CsvTypes.QUOTE) {
            return firstFieldElement.getTextOffset();
        }
        if (CsvHelper.getElementType(lastFieldElement) == CsvTypes.QUOTE) {
            return lastFieldElement.getTextOffset();
        }
        return -1;
    }

    public static int getOpeningQuotePosition(PsiElement errorElement) {
        PsiElement lastFieldElement = errorElement;
        while (CsvHelper.getElementType(lastFieldElement) != CsvTypes.RECORD) {
            lastFieldElement = lastFieldElement.getPrevSibling();
        }
        lastFieldElement = lastFieldElement.getLastChild();
        if (CsvHelper.getElementType(lastFieldElement) != CsvTypes.FIELD) {
            throw new RuntimeException("Field element expected");
        }
        return getOpeningQuotePosition(lastFieldElement.getFirstChild(), lastFieldElement.getLastChild());

    }

    public static PsiElement findQuotePositionsUntilSeparator(PsiElement element, List<Integer> quotePositions) {
        PsiElement separatorElement = null;
        while (separatorElement == null && element != null) {
            if (CsvHelper.getElementType(element) == CsvTypes.COMMA || CsvHelper.getElementType(element) == CsvTypes.CRLF) {
                separatorElement = element;
                continue;
            }
            if (element.getFirstChild() != null) {
                separatorElement = findQuotePositionsUntilSeparator(element.getFirstChild(), quotePositions);
            } else if (element.getText().equals("\"")) {
                quotePositions.add(element.getTextOffset());
            }
            element = element.getNextSibling();
        }
        return separatorElement;
    }

}
