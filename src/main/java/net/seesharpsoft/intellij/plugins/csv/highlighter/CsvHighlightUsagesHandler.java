package net.seesharpsoft.intellij.plugins.csv.highlighter;

import com.intellij.codeInsight.highlighting.HighlightUsagesHandlerBase;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.util.Consumer;
import net.seesharpsoft.intellij.plugins.csv.CsvColumnInfo;
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
        PsiElement myFocusedFieldElement = CsvHelper.getParentFieldElement(this.myFile.getViewProvider().findElementAt(primaryCaret.getOffset()));

        if (myFocusedFieldElement == null) {
            return Collections.emptyList();
        }

        CsvColumnInfo<PsiElement> columnInfo = getCsvFile().getMyColumnInfoMap().getColumnInfo(myFocusedFieldElement);
        return columnInfo == null ? Collections.emptyList() : Collections.unmodifiableList(columnInfo.getElements());
    }

    @Override
    protected void selectTargets(List<PsiElement> list, Consumer<List<PsiElement>> consumer) {
        consumer.consume(list);
    }

    @Override
    public void computeUsages(List<PsiElement> list) {
        list.forEach(element -> {
            if (element != null && !element.getText().isEmpty()) { this.addOccurrence(element); }
        });
    }
}
