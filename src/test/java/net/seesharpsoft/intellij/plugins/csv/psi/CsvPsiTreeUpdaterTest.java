package net.seesharpsoft.intellij.plugins.csv.psi;

import com.intellij.openapi.application.ReadAction;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.LoggedErrorProcessor;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.CsvFileType;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import net.seesharpsoft.intellij.psi.PsiHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
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
                assertNotNull(updater.createCommentFromContent("#comment"));
                assertNotNull(updater.createValueSeparator());
                assertNotNull(updater.createLineBreak());
                assertNotNull(updater.getDocument());
            });
        } finally {
            updater.dispose();
        }
    }

    public void testCommentContentIsDistinctFromCommentSyntax() {
        CsvEditorSettings settings = CsvEditorSettings.getInstance();
        String previousIndicator = settings.getCommentIndicator();
        try {
            for (String indicator : List.of("#", "///")) {
                settings.setCommentIndicator(indicator);
                myFixture.configureByText(CsvFileType.INSTANCE, indicator + "comment\n");
                CsvPsiTreeUpdater updater = new CsvPsiTreeUpdater(myFixture.getFile());
                try {
                    PsiElement comment = updater.createComment(indicator + "comment");
                    assertNotNull(comment);
                    assertEquals(indicator + "comment", comment.getText());
                    for (String content : List.of("", "comment", indicator + "comment", " " + indicator + "comment")) {
                        comment = updater.createCommentFromContent(content);
                        assertNotNull(comment);
                        assertEquals(indicator + content, comment.getText());
                    }
                } finally {
                    updater.dispose();
                }
            }
        } finally {
            settings.setCommentIndicator(previousIndicator);
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

    public void testAddingRowAfterFirstEqualsAddingRowBeforeSecondInPsi() throws Exception {
        String input = "Date,Time,Virtual Memory Committed [MB]\n" +
                "31.1.2026,11:11:56.690,12447\n" +
                "31.1.2026,11:12:0.686,12418\n";

        myFixture.configureByText(CsvFileType.INSTANCE, input);
        PsiFile afterFile = myFixture.getFile();
        CsvPsiTreeUpdater afterUpdater = new CsvPsiTreeUpdater(afterFile);
        String afterPsi;
        String afterColumnPsi;
        try {
            runOnEdt(() -> {
                PsiElement firstRow = ReadAction.compute(() ->
                        PsiHelper.getNthChildOfType(afterFile, 0, CsvRecord.class));
                afterUpdater.addRow(firstRow, false);
                afterUpdater.commit();
            });
            assertEquals("Date,Time,Virtual Memory Committed [MB]\n" +
                    "\n" +
                    "31.1.2026,11:11:56.690,12447\n" +
                    "31.1.2026,11:12:0.686,12418\n", afterFile.getText());
            afterPsi = ReadAction.compute(() -> describePsi(afterFile));
            runOnEdt(() -> {
                afterUpdater.addColumn(0, false);
                afterUpdater.commit();
            });
            assertEquals("Date,,Time,Virtual Memory Committed [MB]\n" +
                    ",\n" +
                    "31.1.2026,,11:11:56.690,12447\n" +
                    "31.1.2026,,11:12:0.686,12418\n", afterFile.getText());
            afterColumnPsi = ReadAction.compute(() -> describePsi(afterFile));
        } finally {
            afterUpdater.dispose();
        }

        myFixture.configureByText(CsvFileType.INSTANCE, input);
        PsiFile beforeFile = myFixture.getFile();
        CsvPsiTreeUpdater beforeUpdater = new CsvPsiTreeUpdater(beforeFile);
        String beforePsi;
        String beforeColumnPsi;
        try {
            runOnEdt(() -> {
                PsiElement secondRow = ReadAction.compute(() ->
                        PsiHelper.getNthChildOfType(beforeFile, 1, CsvRecord.class));
                beforeUpdater.addRow(secondRow, true);
                beforeUpdater.commit();
            });
            assertEquals("Date,Time,Virtual Memory Committed [MB]\n" +
                    "\n" +
                    "31.1.2026,11:11:56.690,12447\n" +
                    "31.1.2026,11:12:0.686,12418\n", beforeFile.getText());
            beforePsi = ReadAction.compute(() -> describePsi(beforeFile));
            runOnEdt(() -> {
                beforeUpdater.addColumn(0, false);
                beforeUpdater.commit();
            });
            assertEquals("Date,,Time,Virtual Memory Committed [MB]\n" +
                    ",\n" +
                    "31.1.2026,,11:11:56.690,12447\n" +
                    "31.1.2026,,11:12:0.686,12418\n", beforeFile.getText());
            beforeColumnPsi = ReadAction.compute(() -> describePsi(beforeFile));
        } finally {
            beforeUpdater.dispose();
        }

        assertEquals(afterPsi, beforePsi);
        assertEquals(afterColumnPsi, beforeColumnPsi);
    }

    public void testAddingColumnAfterQueuedRowBeforeUsesUpdatedPsiRanges() throws Exception {
        myFixture.configureByText(CsvFileType.INSTANCE,
                "Date,Time,Virtual Memory Committed [MB]\n" +
                "31.1.2026,11:11:56.690,12447\n" +
                "31.1.2026,11:12:0.686,12418\n");
        PsiFile file = myFixture.getFile();
        CsvPsiTreeUpdater updater = new CsvPsiTreeUpdater(file);
        try {
            runOnEdt(() -> {
                PsiElement firstRow = ReadAction.compute(() ->
                        PsiHelper.getNthChildOfType(file, 0, CsvRecord.class));
                updater.addRow(firstRow, true);
                updater.addColumn(0, false);
                updater.commit();
            });
            assertEquals(",\n" +
                    "Date,,Time,Virtual Memory Committed [MB]\n" +
                    "31.1.2026,,11:11:56.690,12447\n" +
                    "31.1.2026,,11:12:0.686,12418\n", file.getText());
        } finally {
            updater.dispose();
        }
    }

    public void testAddColumnSkipsRowsWithTooFewFields() throws Exception {
        myFixture.configureByText(CsvFileType.INSTANCE, "one,two,three\n\nfour,five,six\n");
        PsiFile file = myFixture.getFile();
        CsvPsiTreeUpdater updater = new CsvPsiTreeUpdater(file);
        try {
            runOnEdt(() -> {
                updater.addColumn(1, true);
                updater.commit();
            });
            assertEquals("one,,two,three\n\nfour,,five,six\n", file.getText());
        } finally {
            updater.dispose();
        }
    }

    public void testAddColumnAfterSkipsRowsWithTooFewFields() throws Exception {
        myFixture.configureByText(CsvFileType.INSTANCE, "one,two,three\n\nfour,five,six\n");
        PsiFile file = myFixture.getFile();
        CsvPsiTreeUpdater updater = new CsvPsiTreeUpdater(file);
        try {
            runOnEdt(() -> {
                updater.addColumn(1, false);
                updater.commit();
            });
            assertEquals("one,two,,three\n\nfour,five,,six\n", file.getText());
        } finally {
            updater.dispose();
        }
    }

    public void testAddColumnAfterAddingRowBeforeThirdRowUpdatesFinalRow() throws Exception {
        myFixture.configureByText(CsvFileType.INSTANCE,
                "row1a,row1b,row1c,row1d,row1e\n" +
                "row2a,row2b,row2c,row2d,row2e\n" +
                "row3a,row3b,row3c,row3d,row3e\n" +
                "row4a,row4b,row4c,row4d,row4e\n");
        PsiFile file = myFixture.getFile();
        CsvPsiTreeUpdater updater = new CsvPsiTreeUpdater(file);
        try {
            runOnEdt(() -> {
                PsiElement thirdRow = ReadAction.compute(() -> PsiHelper.getNthChildOfType(file, 2, CsvRecord.class));
                assertEquals("row3a,row3b,row3c,row3d,row3e", thirdRow.getText());
                updater.addRow(thirdRow, true);
                updater.commit();

                updater.addColumn(2, true);
                updater.commit();
            });
            assertEquals("row1a,row1b,,row1c,row1d,row1e\n" +
                    "row2a,row2b,,row2c,row2d,row2e\n" +
                    "\n" +
                    "row3a,row3b,,row3c,row3d,row3e\n" +
                    "row4a,row4b,,row4c,row4d,row4e\n", file.getText());
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

    private static String describePsi(@NotNull PsiElement element) {
        StringBuilder result = new StringBuilder();
        appendPsiDescription(element, result);
        return result.toString();
    }

    private static void appendPsiDescription(@NotNull PsiElement element, @NotNull StringBuilder result) {
        result.append(element.getClass().getName())
                .append('|').append(element.getNode().getElementType())
                .append('|').append(element.getTextRange())
                .append('|').append(element.getText().replace("\\", "\\\\")
                        .replace("\r", "\\r").replace("\n", "\\n"))
                .append('\n');
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            appendPsiDescription(child, result);
        }
    }
}
