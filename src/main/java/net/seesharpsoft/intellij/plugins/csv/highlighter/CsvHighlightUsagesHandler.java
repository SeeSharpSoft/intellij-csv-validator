package net.seesharpsoft.intellij.plugins.csv.highlighter;

import com.intellij.codeInsight.highlighting.HighlightUsagesHandlerBase;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.util.Consumer;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfo;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfoMap;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class CsvHighlightUsagesHandler extends HighlightUsagesHandlerBase {

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
        CsvColumnInfoMap<PsiElement> columnInfoMap = getCsvFile().getColumnInfoMap();
        for (PsiElement listElement : (List<PsiElement>)list) {
            CsvColumnInfo<PsiElement> csvColumnInfo = getCsvFile().getColumnInfoMap().getColumnInfo(listElement);
            if (csvColumnInfo == null) {
                continue;
            }
            csvColumnInfo.getElements().forEach(element -> this.addOccurrence(columnInfoMap.getRowInfo(element)));
        }

    }

    @Override
    protected void selectTargets(List list, Consumer consumer) {
        consumer.consume(list);
    }

    protected void addOccurrence(CsvColumnInfo<PsiElement>.RowInfo rowInfo) {
        if (rowInfo == null) {
            return;
        }
        TextRange range = rowInfo.getTextRange();
        if (range != null && range.getLength() > 0) {
            this.myReadUsages.add(range);
        }
    }
}
