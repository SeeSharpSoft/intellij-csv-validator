package net.seesharpsoft.intellij.plugins.csv.highlighter;

import com.intellij.codeInsight.highlighting.HighlightUsagesHandlerBase;
import com.intellij.lang.injection.InjectedLanguageManager;
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

public class CsvHighlightUsagesHandler extends HighlightUsagesHandlerBase<PsiElement> {

    protected CsvHighlightUsagesHandler(@NotNull Editor editor, @NotNull CsvFile file) {
        super(editor, file);
    }

    protected CsvFile getCsvFile() {
        return (CsvFile)this.myFile;
    }

    @Override
    public List<PsiElement> getTargets() {
        Caret primaryCaret = this.myEditor.getCaretModel().getPrimaryCaret();
        PsiElement myFocusedElement = this.myFile.getViewProvider().findElementAt(primaryCaret.getOffset());
        if (myFocusedElement == null) {
            myFocusedElement = this.myFile.getLastChild();
        }
        myFocusedElement = CsvHelper.getParentFieldElement(myFocusedElement);

        if (myFocusedElement == null) {
            return Collections.emptyList();
        }

        CsvColumnInfo<PsiElement> columnInfo = getCsvFile().getMyColumnInfoMap().getColumnInfo(myFocusedElement);
        return columnInfo == null ? Collections.emptyList() : Collections.unmodifiableList(columnInfo.getElements());
    }

    @Override
    protected void selectTargets(List<PsiElement> list, Consumer<List<PsiElement>> consumer) {
        consumer.consume(list);
    }

    @Override
    public void computeUsages(List<PsiElement> list) {
        CsvColumnInfoMap<PsiElement> columnInfoMap = getCsvFile().getMyColumnInfoMap();
        list.forEach(element -> {
            if (element != null && !element.getText().isEmpty()) { this.addOccurrence(columnInfoMap.getRowInfo(element)); }
        });
    }

    protected void addOccurrence(CsvColumnInfo<PsiElement>.RowInfo rowInfo) {
        if (rowInfo == null) {
            return;
        }
        TextRange range = rowInfo.getTextRange();
        if (range != null) {
            PsiElement element = rowInfo.getElement();
            range = InjectedLanguageManager.getInstance(element.getProject()).injectedToHost(element, range);
            this.myReadUsages.add(range);
        }
    }
}
