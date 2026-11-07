package net.seesharpsoft.intellij.plugins.csv.intention;

import com.intellij.psi.PsiElement;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import net.seesharpsoft.intellij.psi.PsiHelper;

import java.util.List;

public final class CsvIntentionHelper {

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
    }
}
