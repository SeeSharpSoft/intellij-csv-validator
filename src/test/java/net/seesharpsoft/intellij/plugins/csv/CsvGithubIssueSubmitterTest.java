package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.testFramework.UsefulTestCase;
import org.jetbrains.plugins.github.api.GithubApiRequest;
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor;
import org.jetbrains.plugins.github.api.data.GithubResponsePage;
import org.jetbrains.plugins.github.api.data.GithubSearchedIssue;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CsvGithubIssueSubmitterTest extends UsefulTestCase {

    // for accessing protected methods during test
    class CsvGithubIssueSubmitterSubClass extends CsvGithubIssueSubmitter {
    }

    class DummyException extends Throwable {
        public DummyException(String message) {
            super(message);
        }

        public void printStackTrace(PrintStream stream) {
            stream.println(this.getMessage());
        }

        public void printStackTrace(PrintWriter writer) {
            writer.println(this.getMessage());
        }
    }

    private CsvGithubIssueSubmitterSubClass classUnderTest = new CsvGithubIssueSubmitterSubClass();

    public void testSearchExistingIssuesNeedle() throws Exception {
        assertEquals("\"crash\"", classUnderTest.searchExistingIssuesNeedle(null));
        assertEquals("\"crash\"", classUnderTest.searchExistingIssuesNeedle(""));
        assertEquals("\"crash\"", classUnderTest.searchExistingIssuesNeedle("   "));
        assertEquals("\"test\"", classUnderTest.searchExistingIssuesNeedle("test"));
        assertEquals("\"test\"", classUnderTest.searchExistingIssuesNeedle("[Automated Report] test"));
        assertEquals("\"[Automated Report]\"", classUnderTest.searchExistingIssuesNeedle("[Automated Report]"));
    }

    public void testSearchExistingIssuesNeedleQuotesQuerySyntax() {
        assertEquals("\"java.lang.IllegalStateException: Unexpected token repo:missing OR NOT is:pr\"",
                classUnderTest.searchExistingIssuesNeedle("[Automated Report] java.lang.IllegalStateException: Unexpected token repo:missing OR NOT is:pr"));
        assertEquals("\"Unexpected token repo:missing C: path\"",
                classUnderTest.searchExistingIssuesNeedle("[Automated Report] Unexpected token \"repo:missing\" C:\\path\\"));
        assertEquals("\"Unexpected token\"", classUnderTest.searchExistingIssuesNeedle("Unexpected\n\t token \""));
        assertEquals("\"crash\"", classUnderTest.searchExistingIssuesNeedle("\"\\ \t"));
    }

    public void testSearchExistingIssuesNeedleLengthLimit() {
        String longWord = "a".repeat(251);
        assertEquals("\"" + "a".repeat(250) + "\"", classUnderTest.searchExistingIssuesNeedle(longWord));
        assertEquals("\"Unexpected\"", classUnderTest.searchExistingIssuesNeedle("Unexpected " + longWord));
        assertEquals("\"" + "a".repeat(250) + "\"", classUnderTest.searchExistingIssuesNeedle("   " + longWord));
    }

    public void testSearchExistingIssuesUsesLiteralQueryAndMatchesFullTitle() throws Exception {
        String title = "[Automated Report] java.lang.IllegalStateException: Unexpected \"repo:missing\" OR NOT is:pr";
        GithubSearchedIssue otherIssue = mock(GithubSearchedIssue.class);
        when(otherIssue.getTitle()).thenReturn(title + " (different error)");
        GithubSearchedIssue duplicateIssue = mock(GithubSearchedIssue.class);
        when(duplicateIssue.getTitle()).thenReturn(title);
        when(duplicateIssue.getNumber()).thenReturn(1078L);
        GithubApiRequestExecutor executor = mock(GithubApiRequestExecutor.WithTokenAuth.class);
        ProgressIndicator indicator = new EmptyProgressIndicator();
        when(executor.execute(any(), any())).thenAnswer(invocation -> {
            GithubApiRequest<?> request = invocation.getArgument(1);
            String url = URLDecoder.decode(request.getUrl(), StandardCharsets.UTF_8);
            assertTrue(url.contains("repo:SeeSharpSoft/intellij-csv-validator"));
            assertTrue(url.contains("state:open"));
            assertTrue(url.contains("\"java.lang.IllegalStateException: Unexpected repo:missing OR NOT is:pr\""));
            return new GithubResponsePage<>(List.of(otherIssue, duplicateIssue), null, null, null, null);
        });

        assertEquals("1078", classUnderTest.searchExistingIssues(executor, title, indicator));
    }

    public void testSearchExistingIssuesDoesNotMatchDifferentTitle() throws Exception {
        GithubSearchedIssue otherIssue = mock(GithubSearchedIssue.class);
        when(otherIssue.getTitle()).thenReturn("[Automated Report] Unexpected \"other token\"");
        GithubApiRequestExecutor executor = mock(GithubApiRequestExecutor.WithTokenAuth.class);
        when(executor.execute(any(), any())).thenAnswer(invocation ->
                new GithubResponsePage<>(List.of(otherIssue), null, null, null, null));

        assertNull(classUnderTest.searchExistingIssues(executor, "[Automated Report] Unexpected \"token\"", new EmptyProgressIndicator()));
    }

    public void testGetIssueTitle() {
        assertEquals("[Automated Report] Test", classUnderTest.getIssueTitle(new IdeaLoggingEvent("Test", new DummyException("Test"))));
        assertEquals("[Automated Report] Unhandled exception in [CoroutineName(com.intellij.openapi.fileEditor.impl.PsiAwareFileEditorManagerImpl), StandaloneCoroutine{Cancelling}, Dispatchers.Default]", classUnderTest.getIssueTitle(new IdeaLoggingEvent("Test", new DummyException("Unhandled exception in [CoroutineName(com.intellij.openapi.fileEditor.impl.PsiAwareFileEditorManagerImpl), StandaloneCoroutine{Cancelling}@5cfe3e69, Dispatchers.Default]"))));
        assertEquals("[Automated Report] An invalid state was detected that occurs if the key's equals or hashCode was modified while it resided in the cache. This violation of the Map contract can lead to non-deterministic behavior (key: com.intellij.psi.impl.ElementBase$ElementIconRequest, key type: ElementIconRequest, node type: PSMS, cache type: SSMS).",
                classUnderTest.getIssueTitle(new IdeaLoggingEvent("", new DummyException("An invalid state was detected that occurs if the key's equals or hashCode was modified while it resided in the cache. This violation of the Map contract can lead to non-deterministic behavior (key: com.intellij.psi.impl.ElementBase$ElementIconRequest@e2330a, key type: ElementIconRequest, node type: PSMS, cache type: SSMS)."))));
    }

    public void testRecentSent() {
        assertEquals(false, classUnderTest.reportWasRecentlySent());
        classUnderTest.reportWasSent();
        assertEquals(true, classUnderTest.reportWasRecentlySent());
    }
}
