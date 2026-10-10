package net.seesharpsoft.intellij.plugins.csv.intention;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfo;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfoMap;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.components.CsvValueSeparator;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public abstract class CsvShiftColumnIntentionAction extends CsvIntentionAction {

    protected CsvShiftColumnIntentionAction(String text) {
        super(text);
    }

    protected static void changeColumnOrder(@NotNull Project project,
                                                  @NotNull CsvFile csvFile,
                                                  @NotNull PsiElement element,
                                                  boolean shiftLeft) {
        List<Pair<TextRange, String>> replacements = ReadAction.compute(() -> {
            if (!element.isValid()) return null;
            Document document = PsiDocumentManager.getInstance(project).getDocument(csvFile);
            if (document == null) return null;

            CsvColumnInfoMap<PsiElement> columnInfoMap = CsvHelper.createColumnInfoMap(csvFile);
            CsvColumnInfo<PsiElement> currentColumn = columnInfoMap.getColumnInfo(element);
            if (currentColumn == null) return null;

            int adjacentColumnIndex = currentColumn.getColumnIndex() + (shiftLeft ? -1 : 1);
            CsvColumnInfo<PsiElement> adjacentColumn = columnInfoMap.getColumnInfo(adjacentColumnIndex);
            if (adjacentColumn == null) return null;

            CsvColumnInfo<PsiElement> leftColumn = shiftLeft ? adjacentColumn : currentColumn;
            CsvColumnInfo<PsiElement> rightColumn = shiftLeft ? currentColumn : adjacentColumn;
            String newText = changeLeftAndRightColumnOrder(
                    document.getText(), CsvHelper.getValueSeparator(csvFile), leftColumn, rightColumn);
            if (document.getText().equals(newText)) return null;

            return Collections.singletonList(Pair.create(TextRange.create(0, document.getTextLength()), newText));
        });
        if (replacements != null) CsvIntentionHelper.applyReplacements(csvFile, replacements);
    }

    @NotNull
    protected static String changeLeftAndRightColumnOrder(String text, CsvValueSeparator separator, CsvColumnInfo<PsiElement> leftColumnInfo, CsvColumnInfo<PsiElement> rightColumnInfo) {
        List<PsiElement> rightElements = rightColumnInfo.getElements();
        List<PsiElement> leftElements = leftColumnInfo.getElements();
        int lastIndex = 0;
        int maxRows = leftElements.size();
        StringBuilder newText = new StringBuilder();

        for (int row = 0; row < maxRows; ++row) {
            PsiElement leftElement = leftElements.get(row);
            if (leftElement == null) {
                continue;
            }
            PsiElement rightElement = rightElements.size() > row ? rightElements.get(row) : null;

            TextRange leftSeparator = findPreviousSeparatorOrCRLF(leftElement);
            TextRange middleSeparator = findNextSeparatorOrCRLF(leftElement);
            TextRange rightSeparator = rightElement == null ?
                    TextRange.create(middleSeparator.getEndOffset(), middleSeparator.getEndOffset()) :
                    findNextSeparatorOrCRLF(rightElement);

            newText.append(text, lastIndex, leftSeparator.getEndOffset())
                    .append(text, middleSeparator.getEndOffset(), rightSeparator.getStartOffset())
                    .append(separator.getCharacter())
                    .append(text, leftSeparator.getEndOffset(), middleSeparator.getStartOffset());

            lastIndex = rightSeparator.getStartOffset();
        }
        newText.append(text.substring(lastIndex));
        return newText.toString();
    }

    protected static TextRange findPreviousSeparatorOrCRLF(PsiElement psiElement) {
        TextRange textRange;
        PsiElement separator = CsvHelper.getPreviousSeparator(psiElement);
        if (separator == null) {
            separator = CsvHelper.getPreviousCRLF(psiElement.getParent());
            if (separator == null) {
                separator = psiElement.getParent().getParent().getFirstChild();
                textRange = TextRange.create(separator.getTextRange().getStartOffset(), separator.getTextRange().getStartOffset());
            } else {
                textRange = separator.getTextRange();
            }
        } else {
            textRange = separator.getTextRange();
        }
        return textRange;
    }

    protected static TextRange findNextSeparatorOrCRLF(PsiElement psiElement) {
        TextRange textRange;
        PsiElement separator = CsvHelper.getNextSeparator(psiElement);
        if (separator == null) {
            separator = CsvHelper.getNextCRLF(psiElement.getParent());
            if (separator == null) {
                separator = psiElement.getParent().getParent().getLastChild();
                textRange = TextRange.create(separator.getTextRange().getEndOffset(), separator.getTextRange().getEndOffset());
            } else {
                textRange = TextRange.create(separator.getTextRange().getStartOffset(), separator.getTextRange().getStartOffset());
            }
        } else {
            textRange = separator.getTextRange();
        }
        return textRange;
    }
}
