package net.seesharpsoft.intellij.plugins.csv.intention;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import net.seesharpsoft.intellij.plugins.csv.CsvBasePlatformTestCase;
import net.seesharpsoft.intellij.plugins.csv.components.CsvEscapeCharacter;
import net.seesharpsoft.intellij.plugins.csv.components.CsvFileAttributes;
import net.seesharpsoft.intellij.plugins.csv.components.CsvValueSeparator;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvPsiTreeUpdater;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

public class CsvIntentionTest extends CsvBasePlatformTestCase {

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/intention";
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        resetFileAttributes();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            resetFileAttributes();
            CsvEditorSettings.getInstance().setDefaultEscapeCharacter(CsvEditorSettings.ESCAPE_CHARACTER_DEFAULT);
        } finally {
            super.tearDown();
        }
    }

    private void resetFileAttributes() {
        CsvFileAttributes attributes = getProject().getServiceIfCreated(CsvFileAttributes.class);
        if (attributes != null) attributes.reset();
    }

    protected void doTestIntention(String testName, String hint) throws Throwable {
        doTestIntention(testName, hint, false);
    }

    protected void doTestIntention(String testName, String hint, boolean expectError) throws Throwable {
        myFixture.configureByFile(testName + "/before.csv");
        final IntentionAction action = myFixture.filterAvailableIntentions(hint).stream()
                .filter(intentionAction -> intentionAction.getText().equals(hint))
                .findFirst().orElse(null);
        if (action == null) {
            assertTrue("action not found -> this was expected: " + expectError, expectError);
        } else {
            assertFalse("action was found -> this was expected: " + !expectError, expectError);
            Document document = myFixture.getDocument(myFixture.getFile());
            AtomicInteger documentChanges = new AtomicInteger();
            DocumentListener listener = new DocumentListener() {
                @Override
                public void documentChanged(@NotNull DocumentEvent event) {
                    documentChanges.incrementAndGet();
                }
            };
            document.addDocumentListener(listener);
            try {
                myFixture.launchAction(action);
            } finally {
                document.removeDocumentListener(listener);
            }
            myFixture.checkResultByFile(testName + "/after.csv");
            assertTrue("Each intention action must produce not more than one document change", documentChanges.get() < 2);
        }
    }

    public void testErroneousCsv() throws Throwable {
        doTestIntention("Erroneous", "Quote All", true);
    }

    public void testErroneousBackslashCsv() throws Throwable {
        CsvEditorSettings.getInstance().setDefaultEscapeCharacter(CsvEscapeCharacter.BACKSLASH);
        doTestIntention("ErroneousBackslash", "Quote All", true);
    }

    public void testQuoteAllIntention() throws Throwable {
        doTestIntention("QuoteAll", "Quote All");
    }

    public void testQuoteAllBackslashIntention() throws Throwable {
        CsvEditorSettings.getInstance().setDefaultEscapeCharacter(CsvEscapeCharacter.BACKSLASH);
        doTestIntention("QuoteAllBackslash", "Quote All");
    }

    public void testUnquoteAllIntention() throws Throwable {
        doTestIntention("UnquoteAll", "Unquote All");
    }

    public void testUnquoteAllBackslashIntention() throws Throwable {
        CsvEditorSettings.getInstance().setDefaultEscapeCharacter(CsvEscapeCharacter.BACKSLASH);
        doTestIntention("UnquoteAllBackslash", "Unquote All");
    }

    public void testQuoteIntention() throws Throwable {
        doTestIntention("QuoteValue", "Quote");
    }

    public void testUnquoteIntention() throws Throwable {
        doTestIntention("UnquoteValue", "Unquote");
    }

    public void testShiftColumnLeftIntention() throws Throwable {
        for (int i = 1; i < 5; ++i) {
            doTestIntention(String.format("ShiftColumnLeft%02d", i), "Shift Column Left");
        }
    }

    public void testShiftColumnRightIntention() throws Throwable {
        for (int i = 1; i < 5; ++i) {
            doTestIntention(String.format("ShiftColumnRight%02d", i), "Shift Column Right");
        }
    }

    public void testColumnShiftsPreserveCommentLikeValuesAndTerminalNewlineLikeTableMoves() {
        checkShiftMatchesTable("data.csv", "a, # <caret>value,b\n# actual comment\n",
                "Shift Column Left", new int[]{1, 0, 2});
        checkShiftMatchesTable("data.csv", "a,<caret>b,c\n1\n\n",
                "Shift Column Left", new int[]{1, 0, 2});
    }

    public void testColumnShiftsPreserveTsvAndPsvValuesLikeTableMoves() {
        checkShiftMatchesTable("data.tsv", "a\t<caret>\"multi\nline\"\tc\n1\t2\n",
                "Shift Column Left", new int[]{1, 0, 2});
        checkShiftMatchesTable("data.psv", "<caret>a|\"b|c\"|d\n1|2\n",
                "Shift Column Right", new int[]{1, 0, 2});
    }

    public void testColumnShiftsPreserveCustomSeparatorAndEscapeLikeTableMoves() {
        CsvEditorSettings settings = CsvEditorSettings.getInstance();
        CsvValueSeparator previousSeparator = settings.getDefaultValueSeparator();
        boolean previousDetection = settings.isAutoDetectValueSeparator();
        try {
            settings.setAutoDetectValueSeparator(false);
            settings.setDefaultValueSeparator(new CsvValueSeparator("$~"));
            settings.setDefaultEscapeCharacter(CsvEscapeCharacter.BACKSLASH);
            checkShiftMatchesTable("data.csv", "a$~<caret>\"b\\\"c\"$~d\n1$~2\n",
                    "Shift Column Right", new int[]{0, 2, 1});
        } finally {
            settings.setDefaultValueSeparator(previousSeparator);
            settings.setAutoDetectValueSeparator(previousDetection);
        }

    }

    public void testColumnShiftsAreUnavailableOnCommentsAndSyntaxErrors() {
        for (String text : new String[]{"# <caret>comment\na,b", "a,<caret>\"unclosed"}) {
            myFixture.configureByText("data.csv", text);
            assertEmpty(myFixture.filterAvailableIntentions("Shift Column Left"));
            assertEmpty(myFixture.filterAvailableIntentions("Shift Column Right"));
        }
    }

    private void checkShiftMatchesTable(String name, String text, String hint, int[] order) {
        myFixture.configureByText(name, text);
        myFixture.launchAction(myFixture.findSingleIntention(hint));
        String intentionResult = myFixture.getEditor().getDocument().getText();

        myFixture.configureByText(name, text.replace("<caret>", ""));
        CsvPsiTreeUpdater updater = new CsvPsiTreeUpdater(myFixture.getFile());
        try {
            updater.reorderColumns(order);
            updater.commit();
        } finally {
            updater.dispose();
        }
        assertEquals("Text and table moves must produce the same CSV document",
                myFixture.getEditor().getDocument().getText(), intentionResult);
    }
}
