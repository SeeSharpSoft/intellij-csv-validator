package net.seesharpsoft.intellij.plugins.csv.highlighter;

import com.intellij.codeInsight.highlighting.HighlightUsagesHandlerBase;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.util.Consumer;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvField;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvRecord;
import net.seesharpsoft.intellij.psi.PsiHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class CsvHighlightUsagesHandler extends HighlightUsagesHandlerBase {

    private static final int MAX_HIGHLIGHTING_ROWS = 1000;

    protected CsvHighlightUsagesHandler(@NotNull Editor editor, @NotNull CsvFile file) {
        super(editor, file);
    }

    protected CsvFile getCsvFile() {
        return (CsvFile) this.myFile;
    }

    @Override
    public List<PsiElement> getTargets() {
        if (!this.myEditor.getSelectionModel().hasSelection()) {
            return Collections.emptyList();
        }

        Caret primaryCaret = this.myEditor.getCaretModel().getPrimaryCaret();
        PsiElement myFocusedElement = this.myFile.getViewProvider().findElementAt(primaryCaret.getOffset());
        if (myFocusedElement == null) {
            myFocusedElement = this.myFile.getLastChild();
        }
        myFocusedElement = CsvHelper.getParentFieldElement(myFocusedElement);

        if (myFocusedElement == null) {
            return Collections.emptyList();
        }

        return Collections.singletonList(myFocusedElement);
    }

    @Override
    public void computeUsages(List list) {
        for (PsiElement psiElement : (List<PsiElement>) list) {
            int index = CsvHelper.getFieldIndex(psiElement);
            if (index == -1) continue;
            CsvRecord csvRecord = (CsvRecord) psiElement.getParent();
            int leftOverRows = addOccurrence(csvRecord, index, MAX_HIGHLIGHTING_ROWS / 2, false);
            addOccurrence(csvRecord, index, MAX_HIGHLIGHTING_ROWS / 2 + leftOverRows, true);
        }
    }

    @Override
    protected void selectTargets(List list, Consumer consumer) {
        consumer.consume(list);
    }

    protected int addOccurrence(@NotNull CsvRecord csvRecord, int index, int noOfSiblings, boolean backward) {
        int count = noOfSiblings;
        for (PsiElement sibling = backward ? csvRecord.getPrevSibling() : csvRecord;
             count != 0 && sibling != null;
             sibling = backward ? sibling.getPrevSibling() : sibling.getNextSibling(), --count) {
            PsiElement field = PsiHelper.getNthChildOfType(sibling, index, CsvField.class);
            if (field != null) {
                TextRange range = field.getTextRange();
                if (range != null && range.getLength() > 0) {
                    myReadUsages.add(range);
                }
            }
        }
        return count;
    }
}
