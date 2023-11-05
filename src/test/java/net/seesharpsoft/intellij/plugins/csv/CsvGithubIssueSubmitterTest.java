package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.testFramework.UsefulTestCase;

import java.io.PrintStream;
import java.io.PrintWriter;

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
