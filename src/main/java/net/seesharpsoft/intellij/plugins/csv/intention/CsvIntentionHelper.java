package net.seesharpsoft.intellij.plugins.csv.intention;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvField;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvPsiTreeUpdater;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import net.seesharpsoft.intellij.psi.PsiHelper;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class CsvIntentionHelper {

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
        while (!todo.isEmpty()) {
            PsiElement current = todo.getLast();
            todo.removeLast();
            elements.add(current);
            todo.addAll(getChildren(current));
        }
        return elements;
    }

    public static void quoteAll(@NotNull PsiFile psiFile) {
        applyReplacements(psiFile, ReadAction.compute(() -> collectQuoteAllReplacements(psiFile)));
    }

    private static List<Pair<TextRange, String>> collectQuoteAllReplacements(@NotNull PsiFile psiFile) {
        List<Pair<TextRange, String>> replacements = new ArrayList<>();
        PsiTreeUtil.processElements(psiFile, CsvField.class, field -> {
            if (PsiHelper.getElementType(field.getFirstChild()) != CsvTypes.QUOTE) {
                replacements.add(Pair.create(TextRange.create(field.getTextRange().getStartOffset(), field.getTextRange().getStartOffset()), "\""));
            }
            if (PsiHelper.getElementType(field.getLastChild()) != CsvTypes.QUOTE) {
                replacements.add(Pair.create(TextRange.create(field.getTextRange().getEndOffset(), field.getTextRange().getEndOffset()), "\""));
            }
            return true;
        });
        return replacements;
    }

    public static void quoteValue(@NotNull final PsiElement field) {
        Pair<PsiFile, List<Pair<TextRange, String>>> fileAndReplacements = ReadAction.compute(() -> {
            List<Pair<TextRange, String>> result = new ArrayList<>();
            if (PsiHelper.getElementType(field.getFirstChild()) != CsvTypes.QUOTE) {
                result.add(Pair.create(TextRange.create(field.getTextRange().getStartOffset(), field.getTextRange().getStartOffset()), "\""));
            }
            if (PsiHelper.getElementType(field.getLastChild()) != CsvTypes.QUOTE) {
                result.add(Pair.create(TextRange.create(field.getTextRange().getEndOffset(), field.getTextRange().getEndOffset()), "\""));
            }
            return Pair.create(field.getContainingFile(), result);
        });
        applyReplacements(fileAndReplacements.getFirst(), fileAndReplacements.getSecond());
    }

    public static void unquoteAll(@NotNull PsiFile psiFile) {
        applyReplacements(psiFile, ReadAction.compute(() -> collectUnquoteAllReplacements(psiFile)));
    }

    private static List<Pair<TextRange, String>> collectUnquoteAllReplacements(@NotNull PsiFile psiFile) {
        final List<Pair<TextRange, String>> replacements = new ArrayList<>();
        PsiTreeUtil.processElements(psiFile, CsvField.class, field -> {
            if (getChildren(field).stream().noneMatch(element -> PsiHelper.getElementType(element) == CsvTypes.ESCAPED_TEXT)) {
                Pair<PsiElement, PsiElement> positions = getQuotePositions(field);
                if (positions != null) {
                    replacements.add(Pair.create(positions.getFirst().getTextRange(), ""));
                    replacements.add(Pair.create(positions.getSecond().getTextRange(), ""));
                }
            }
            return true;
        });

        return replacements;
    }

    public static void unquoteValue(@NotNull final PsiElement field) {
        Pair<PsiFile, List<PsiElement>> fileAndQuotes = ReadAction.compute(() ->
                Pair.create(field.getContainingFile(), getQuoteElements(field)));
        removeQuotes(fileAndQuotes.getFirst(), fileAndQuotes.getSecond());
    }

    private static List<PsiElement> getQuoteElements(@NotNull PsiElement field) {
        if (getChildren(field).stream().anyMatch(element -> PsiHelper.getElementType(element) == CsvTypes.ESCAPED_TEXT)) {
            return Collections.emptyList();
        }
        Pair<PsiElement, PsiElement> positions = getQuotePositions(field);
        return positions == null ? Collections.emptyList() : Arrays.asList(positions.getFirst(), positions.getSecond());
    }

    public static void applyReplacements(@NotNull PsiFile psiFile, List<Pair<TextRange, String>> replacements) {
        if (replacements.isEmpty()) return;
        CsvPsiTreeUpdater updater = new CsvPsiTreeUpdater(psiFile);
        try {
            updater.replaceTexts(replacements);
            updater.commit();
        } finally {
            updater.dispose();
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

    public static void addQuotes(@NotNull PsiFile psiFile, List<Integer> quotePositions) {
        List<Pair<TextRange, String>> replacements = new ArrayList<>();
        for (int position : quotePositions) {
            replacements.add(Pair.create(TextRange.create(position, position), "\""));
        }
        applyReplacements(psiFile, replacements);
    }

    public static void removeQuotes(@NotNull PsiFile psiFile, List<PsiElement> quoteElements) {
        List<Pair<TextRange, String>> replacements = ReadAction.compute(() -> {
            List<Pair<TextRange, String>> result = new ArrayList<>();
            for (PsiElement element : quoteElements) {
                result.add(Pair.create(element.getTextRange(), ""));
            }
            return result;
        });
        applyReplacements(psiFile, replacements);
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
