package net.seesharpsoft.intellij.plugins.csv.intention;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvField;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import net.seesharpsoft.intellij.psi.PsiHelper;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public final class CsvIntentionHelper {

//    private static final Logger LOG = Logger.getInstance("#net.seesharpsoft.intellij.plugins.csv.inspection.CsvIntentionHelper");

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
        Collection<PsiElement> elements = new HashSet<>();
        while (todo.size() > 0) {
            PsiElement current = todo.get(todo.size() - 1);
            todo.remove(todo.size() - 1);
            elements.add(current);
            todo.addAll(getChildren(current));
        }
        return elements;
    }

    public static void quoteAll(@NotNull Project project, @NotNull PsiFile psiFile) {
        Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
        List<Integer> quotePositions = new ArrayList<>();

        PsiTreeUtil.processElements(psiFile, CsvField.class, field -> {
            if (PsiHelper.getElementType(field.getFirstChild()) != CsvTypes.QUOTE) {
                quotePositions.add(field.getTextRange().getStartOffset());
            }
            if (PsiHelper.getElementType(field.getLastChild()) != CsvTypes.QUOTE) {
                quotePositions.add(field.getTextRange().getEndOffset());
            }
            return true;
        });
        addQuotes(document, quotePositions);
    }

    public static void quoteValue(@NotNull Project project, @NotNull final PsiElement field) {
        Document document = PsiDocumentManager.getInstance(project).getDocument(field.getContainingFile());
        List<Integer> quotePositions = new ArrayList<>();
        if (PsiHelper.getElementType(field.getFirstChild()) != CsvTypes.QUOTE) {
            quotePositions.add(field.getTextRange().getStartOffset());
        }
        if (PsiHelper.getElementType(field.getLastChild()) != CsvTypes.QUOTE) {
            quotePositions.add(field.getTextRange().getEndOffset());
        }
        addQuotes(document, quotePositions);
    }

    public static void unquoteAll(@NotNull Project project, @NotNull PsiFile psiFile) {
        Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);

        final List<PsiElement> quotePositions = new ArrayList<>();

        PsiTreeUtil.processElements(psiFile, CsvField.class, field -> {
            if (getChildren(field).stream().noneMatch(element -> PsiHelper.getElementType(element) == CsvTypes.ESCAPED_TEXT)) {
                Pair<PsiElement, PsiElement> positions = getQuotePositions(field);
                if (positions != null) {
                    quotePositions.add(positions.getFirst());
                    quotePositions.add(positions.getSecond());
                }
            }
            return true;
        });

        removeQuotes(document, quotePositions);
    }

    public static void unquoteValue(@NotNull Project project, @NotNull final PsiElement field) {
        unquoteValue(Objects.requireNonNull(PsiDocumentManager.getInstance(project).getDocument(field.getContainingFile())), field);
    }

    public static void unquoteValue(@NotNull Document document, @NotNull final PsiElement field) {
        if (getChildren(field).stream().anyMatch(element -> PsiHelper.getElementType(element) == CsvTypes.ESCAPED_TEXT)) {
            return;
        }
        Pair<PsiElement, PsiElement> positions = getQuotePositions(field);
        if (positions != null) {
            removeQuotes(document, Arrays.asList(positions.getFirst(), positions.getSecond()));
        }
    }

    private static Pair<PsiElement, PsiElement> getQuotePositions(PsiElement element) {
        PsiElement firstChild = element.getFirstChild();
        PsiElement lastChild = element.getLastChild();
        if (PsiHelper.getElementType(firstChild) == CsvTypes.QUOTE && PsiHelper.getElementType(lastChild) == CsvTypes.QUOTE) {
            return Pair.create(firstChild, lastChild);
        }
        return null;
    }

    public static void addQuotes(final Document document, List<Integer> quotePositions) {
        int offset = 0;
        String quote = "\"";
        quotePositions.sort(Integer::compareTo);
        for (int position : quotePositions) {
            int offsetPosition = position + offset;
            document.insertString(offsetPosition, quote);
            ++offset;
        }
    }

    public static void removeQuotes(final Document document, List<PsiElement> quoteElements) {
        int offset = 0;
        quoteElements.sort(Comparator.comparingInt(PsiElement::getTextOffset));
        for (PsiElement element : quoteElements) {
            int startOffset = element.getTextRange().getStartOffset() + offset;
            int endOffset = startOffset + element.getTextLength();
            document.replaceString(startOffset, endOffset, "");
            offset -= (endOffset - startOffset);
        }
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
