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
import net.seesharpsoft.intellij.psi.PsiHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public final class CsvIntentionHelper {

    private static final Logger LOG = Logger.getInstance("#net.seesharpsoft.intellij.plugins.csv.inspection.CsvIntentionHelper");

    public static List<PsiElement> getChildren(final PsiElement element) {
        PsiElement currentElement = element;
        List<PsiElement> children = new ArrayList<>();
        if (currentElement != null) {
            currentElement = currentElement.getFirstChild();
            while (currentElement != null) {
                children.add(currentElement);
                currentElement = currentElement.getNextSibling();
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
                .filter(element -> PsiHelper.getElementType(element) == CsvTypes.RECORD)
                .flatMap(record -> getChildren(record).stream())
                .filter(element -> PsiHelper.getElementType(element) == CsvTypes.FIELD)
                .collect(Collectors.toList());
    }

    public static void quoteAll(@NotNull Project project, @NotNull PsiFile psiFile) {
        try {
            Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
            List<Integer> quotePositions = new ArrayList<>();
            Collection<PsiElement> fields = getAllFields(psiFile);
            PsiElement separator;
            for (PsiElement field : fields) {
                if (field.getFirstChild() == null || PsiHelper.getElementType(field.getFirstChild()) != CsvTypes.QUOTE) {
                    separator = CsvHelper.getPreviousSeparator(field);
                    if (separator == null) {
                        quotePositions.add(field.getParent().getTextOffset());
                    } else {
                        quotePositions.add(separator.getTextOffset() + separator.getTextLength());
                    }
                }
                if (field.getLastChild() == null || PsiHelper.getElementType(field.getLastChild()) != CsvTypes.QUOTE) {
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
                if (getChildren(field).stream().anyMatch(element -> PsiHelper.getElementType(element) == CsvTypes.ESCAPED_TEXT)) {
                    continue;
                }
                if (PsiHelper.getElementType(field.getFirstChild()) == CsvTypes.QUOTE) {
                    quotePositions.add(field.getFirstChild().getTextOffset());
                }
                if (PsiHelper.getElementType(field.getLastChild()) == CsvTypes.QUOTE) {
                    quotePositions.add(field.getLastChild().getTextOffset());
                }
            }
            String text = removeQuotes(document.getText(), quotePositions);
            document.setText(text);
        } catch (IncorrectOperationException e) {
            LOG.error(e);
        }
    }

    public static void quoteValue(@NotNull Project project, @NotNull final PsiElement element) {
        try {
            Document document = PsiDocumentManager.getInstance(project).getDocument(element.getContainingFile());
            List<Integer> quotePositions = new ArrayList<>();

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

    public static void unquoteValue(@NotNull Project project, @NotNull final PsiElement element) {
        try {
            Document document = PsiDocumentManager.getInstance(project).getDocument(element.getContainingFile());
            List<Integer> quotePositions = new ArrayList<>();

            if (PsiHelper.getElementType(element.getFirstChild()) == CsvTypes.QUOTE) {
                quotePositions.add(element.getFirstChild().getTextOffset());
            }
            if (PsiHelper.getElementType(element.getLastChild()) == CsvTypes.QUOTE) {
                quotePositions.add(element.getLastChild().getTextOffset());
            }
            String text = removeQuotes(document.getText(), quotePositions);
            document.setText(text);
        } catch (IncorrectOperationException e) {
            LOG.error(e);
        }
    }

    public static String addQuotes(final String original, List<Integer> quotePositions) {
        String text = original;
        int offset = 0;
        quotePositions.sort(Integer::compareTo);
        for (int position : quotePositions) {
            int offsetPosition = position + offset;
            text = text.substring(0, offsetPosition) + "\"" + text.substring(offsetPosition);
            ++offset;
        }
        return text;
    }

    public static String removeQuotes(final String original, List<Integer> quotePositions) {
        String text = original;
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
        if (PsiHelper.getElementType(firstFieldElement) != CsvTypes.QUOTE) {
            return firstFieldElement.getTextOffset();
        }
        if (PsiHelper.getElementType(lastFieldElement) == CsvTypes.QUOTE) {
            return lastFieldElement.getTextOffset();
        }
        return -1;
    }

    public static int getOpeningQuotePosition(PsiElement errorElement) {
        PsiElement lastFieldElement = errorElement;
        while (PsiHelper.getElementType(lastFieldElement) != CsvTypes.RECORD) {
            lastFieldElement = lastFieldElement.getPrevSibling();
        }
        lastFieldElement = lastFieldElement.getLastChild();
        if (PsiHelper.getElementType(lastFieldElement) != CsvTypes.FIELD) {
            throw new IllegalArgumentException("Field element expected");
        }
        return getOpeningQuotePosition(lastFieldElement.getFirstChild(), lastFieldElement.getLastChild());
    }

    public static PsiElement findQuotePositionsUntilSeparator(final PsiElement element, List<Integer> quotePositions) {
        return findQuotePositionsUntilSeparator(element, quotePositions, false);
    }

    public static PsiElement findQuotePositionsUntilSeparator(final PsiElement element, List<Integer> quotePositions, boolean stopAtEscapedTexts) {
        PsiElement currentElement = element;
        PsiElement separatorElement = null;
        while (separatorElement == null && currentElement != null) {
            if (PsiHelper.getElementType(currentElement) == CsvTypes.COMMA || PsiHelper.getElementType(currentElement) == CsvTypes.CRLF ||
                    (stopAtEscapedTexts && PsiHelper.getElementType(currentElement) == CsvTypes.ESCAPED_TEXT)) {
                separatorElement = currentElement;
                continue;
            }
            if (currentElement.getFirstChild() != null) {
                separatorElement = findQuotePositionsUntilSeparator(currentElement.getFirstChild(), quotePositions, stopAtEscapedTexts);
            } else if (currentElement.getText().equals("\"")) {
                quotePositions.add(currentElement.getTextOffset());
            }
            currentElement = currentElement.getNextSibling();
        }
        return separatorElement;
    }

    private CsvIntentionHelper() {
        // static utility class
    }
}
