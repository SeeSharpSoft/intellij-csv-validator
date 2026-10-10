package net.seesharpsoft.intellij.plugins.csv.psi;

import com.intellij.openapi.application.ReadAction;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.LoggedErrorProcessor;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.CsvFileType;
import net.seesharpsoft.intellij.psi.PsiHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

public class CsvPsiTreeUpdaterTest extends CsvBasePlatformTestCase {

    public void testPsiCreationFromEdt() throws Exception {
        myFixture.configureByText(CsvFileType.INSTANCE, "one,two\n");
        CsvPsiTreeUpdater updater = new CsvPsiTreeUpdater(myFixture.getFile());
        try {
            runOnEdt(() -> {
                assertNotNull(updater.createField("value", false));
                assertNotNull(updater.createRecord());
                assertNotNull(updater.createComment("comment"));
                assertNotNull(updater.createValueSeparator());
                assertNotNull(updater.createLineBreak());
                assertNotNull(updater.getDocument());
            });
        } finally {
            updater.dispose();
        }
    }

    public void testFieldAndCommentReplacementFromEdt() throws Exception {
        myFixture.configureByText(CsvFileType.INSTANCE, "one,two\n#comment\n");
        PsiFile file = myFixture.getFile();
        CsvPsiTreeUpdater updater = new CsvPsiTreeUpdater(file);
        try {
            runOnEdt(() -> {
                PsiElement field = ReadAction.compute(() -> PsiHelper.getNthChildOfType(file, 0, CsvRecord.class).getFirstChild());
                PsiElement comment = ReadAction.compute(() -> PsiHelper.getNthChildOfType(file, 1, CsvRecord.class).getFirstChild());
                updater.replaceField(field, "changed", false);
                updater.replaceComment(comment, "updated");
                updater.commit();
            });
            assertEquals("changed,two\n#updated\n", file.getText());
        } finally {
            updater.dispose();
        }
    }

    public void testAppendAndRemoveFieldFromEdt() throws Exception {
        myFixture.configureByText(CsvFileType.INSTANCE, "one,two\n");
        PsiFile file = myFixture.getFile();
        CsvPsiTreeUpdater updater = new CsvPsiTreeUpdater(file);
        try {
            runOnEdt(() -> {
                PsiElement field = ReadAction.compute(() -> PsiHelper.getNthChildOfType(file, 0, CsvRecord.class).getFirstChild());
                updater.appendField(field, "three", false);
                updater.commit();

                PsiElement appendedField = ReadAction.compute(() ->
                        PsiHelper.getNthChildOfType(file, 0, CsvRecord.class).getFirstChild().getNextSibling().getNextSibling());
                updater.removeField(appendedField);
                updater.commit();
            });
            assertEquals("one,two\n", file.getText());
        } finally {
            updater.dispose();
        }
    }

    public void testRowAndColumnOperationsFromEdt() throws Exception {
        myFixture.configureByText(CsvFileType.INSTANCE, "one,two\nthree,four\n");
        PsiFile file = myFixture.getFile();
        CsvPsiTreeUpdater updater = new CsvPsiTreeUpdater(file);
        try {
            runOnEdt(() -> {
                PsiElement firstRow = ReadAction.compute(() -> PsiHelper.getNthChildOfType(file, 0, CsvRecord.class));
                updater.addRow(firstRow, false);
                updater.commit();

                updater.addColumn(1, true);
                updater.commit();

                updater.deleteColumns(Collections.singletonList(1));
                updater.commit();

                updater.deleteRows(Collections.singletonList(1));
                updater.commit();
            });
            assertEquals("one,two\nthree,four\n", file.getText());
        } finally {
            updater.dispose();
        }
    }

    public void testDeleteContentFromEdt() throws Exception {
        myFixture.configureByText(CsvFileType.INSTANCE, "one,two\nthree,four\n");
        PsiFile file = myFixture.getFile();
        CsvPsiTreeUpdater updater = new CsvPsiTreeUpdater(file);
        try {
            runOnEdt(() -> {
                updater.deleteContent();
                updater.commit();
            });
            assertEquals("", file.getText());
        } finally {
            updater.dispose();
        }
    }

    private void runOnEdt(@NotNull Runnable operation)  {
        LoggedErrorProcessor.executeWith(new LoggedErrorProcessor() {
            @Override
            public @NotNull Set<Action> processError(@NotNull String category,
                                                      @NotNull String message,
                                                      String @NotNull [] details,
                                                      Throwable t) {
                return Set.of(Action.RETHROW);
            }
        }, () -> EdtTestUtil.runInEdtAndWait(operation::run));
    }
}
