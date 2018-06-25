package net.seesharpsoft.intellij.plugins.csv.annotation;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.cache.CacheManager;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.impl.source.tree.FileElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.ExpectedHighlightingData;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static net.seesharpsoft.intellij.plugins.csv.annotation.CsvAnnotator.CSV_COLUMN_INFO_SEVERITY;

public class CsvAnnotatorTest extends LightCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources";
    }

    public void testAnnotator() {
        myFixture.configureByFile("AnnotatorTestData.csv");
        this.collectAndCheckHighlighting();
    }

    private long collectAndCheckHighlighting() {
        ExpectedHighlightingData data = new ExpectedHighlightingData(myFixture.getEditor().getDocument(), false, false, true, false, this.getHostFile());
        data.registerHighlightingType("csv_column_info", new ExpectedHighlightingData.ExpectedHighlightingSet(CSV_COLUMN_INFO_SEVERITY, false, true));
        data.init();
        return this.collectAndCheckHighlighting(data);
    }

    private PsiFile getHostFile() {
        return myFixture.getFile();
    }

    private long collectAndCheckHighlighting(@NotNull ExpectedHighlightingData data) {
        Project project = myFixture.getProject();
        EdtTestUtil.runInEdtAndWait(() -> {
            PsiDocumentManager.getInstance(project).commitAllDocuments();
        });
        PsiFileImpl file = (PsiFileImpl)this.getHostFile();
        FileElement hardRefToFileElement = file.calcTreeElement();
        if (!DumbService.isDumb(project)) {
            CacheManager.SERVICE.getInstance(project).getFilesWithWord("XXX", (short)2, GlobalSearchScope.allScope(project), true);
        }

        long start = System.currentTimeMillis();
        Disposable disposable = Disposer.newDisposable();

        List infos;
        try {
            infos = myFixture.doHighlighting();
            this.removeDuplicatedRangesForInjected(infos);
        } finally {
            Disposer.dispose(disposable);
        }

        long elapsed = System.currentTimeMillis() - start;
        data.checkResult(infos, file.getText());
        hardRefToFileElement.hashCode();
        return elapsed;
    }

    private static void removeDuplicatedRangesForInjected(@NotNull List<HighlightInfo> infos) {
        Collections.sort(infos, (o1, o2) -> {
            int i = o2.startOffset - o1.startOffset;
            return i != 0 ? i : o1.getSeverity().myVal - o2.getSeverity().myVal;
        });
        HighlightInfo prevInfo = null;

        HighlightInfo info;
        for(Iterator it = infos.iterator(); it.hasNext(); prevInfo = info.type == HighlightInfoType.INJECTED_LANGUAGE_FRAGMENT ? info : null) {
            info = (HighlightInfo)it.next();
            if (prevInfo != null && info.getSeverity() == HighlightInfoType.SYMBOL_TYPE_SEVERITY && info.getDescription() == null && info.startOffset == prevInfo.startOffset && info.endOffset == prevInfo.endOffset) {
                it.remove();
            }
        }

    }

}
