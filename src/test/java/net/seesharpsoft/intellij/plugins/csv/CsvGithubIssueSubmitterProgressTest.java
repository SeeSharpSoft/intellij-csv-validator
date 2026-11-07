package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.util.Computable;
import org.jetbrains.plugins.github.api.GithubApiRequest;
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor;
import org.jetbrains.plugins.github.api.GithubServerPath;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

public class CsvGithubIssueSubmitterProgressTest extends CsvBasePlatformTestCase {

    private static final String TITLE = "[Automated Report] java.lang.Throwable: Test error";

    private final CsvGithubIssueSubmitter classUnderTest = new CsvGithubIssueSubmitter() {
        @Override
        protected String getIssueDetails(IdeaLoggingEvent event, String additionalInfo) {
            return additionalInfo == null ? "Test report details" : additionalInfo;
        }
    };

    public void testNewIssueWithoutAdditionalDetailsKeepsTaskIndicatorRunning() throws Exception {
        assertSubmission(null);
    }

    public void testNewIssueWithAdditionalDetailsKeepsTaskIndicatorRunning() throws Exception {
        assertSubmission("Additional details");
    }

    private void assertSubmission(String additionalInfo) throws Exception {
        ProgressManager progressManager = ProgressManager.getInstance();
        ProgressIndicator taskIndicator = new ProgressIndicatorBase();
        GithubApiRequestExecutor executor = spy(GithubApiRequestExecutor.Factory.getInstance()
                .create(GithubServerPath.DEFAULT_SERVER, "test-token"));
        List<GithubApiRequest<?>> requests = new ArrayList<>();
        List<SubmittedReportInfo> reports = new ArrayList<>();

        doAnswer(invocation -> {
            ProgressIndicator requestIndicator = invocation.getArgument(0);
            assertNotSame(taskIndicator, requestIndicator);
            assertFalse(requestIndicator.isRunning());
            assertTrue(taskIndicator.isRunning());
            assertSame(taskIndicator, progressManager.getProgressIndicator());
            requests.add(invocation.getArgument(1));

            // Reproduce GithubApiHelperRequestExecutor's nested progress lifecycle without HTTP.
            Object result = progressManager.runProcess((Computable<Object>) () -> {
                assertSame(requestIndicator, progressManager.getProgressIndicator());
                assertTrue(requestIndicator.isRunning());
                requestIndicator.setText("Sending report");
                assertEquals("Sending report", taskIndicator.getText());
                return null;
            }, requestIndicator);

            assertFalse(requestIndicator.isRunning());
            assertTrue(taskIndicator.isRunning());
            assertSame(taskIndicator, progressManager.getProgressIndicator());
            return result;
        }).when(executor).execute(any(ProgressIndicator.class), any());

        progressManager.runProcess(() -> {
            classUnderTest.submitToGithub(new IdeaLoggingEvent("Test", new Throwable("Test error")),
                    additionalInfo, executor, reports::add, taskIndicator);
            assertTrue(taskIndicator.isRunning());
        }, taskIndicator);

        assertFalse(taskIndicator.isRunning());
        assertEquals(1, requests.size());
        assertEquals(1, reports.size());
        assertEquals(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE, reports.getFirst().getStatus());
        GithubApiRequest.Post<?> request = assertInstanceOf(requests.getFirst(), GithubApiRequest.Post.class);
        assertEquals("https://api.github.com/repos/SeeSharpSoft/intellij-csv-validator/issues", request.getUrl());
        assertNotNull(request.getBody());
        assertTrue(request.getBody().contains(TITLE));
        assertTrue(request.getBody().contains(additionalInfo == null ? "Test report details" : additionalInfo));
    }

    public void testTaskCancellationReachesRequestIndicator() throws Exception {
        ProgressManager progressManager = ProgressManager.getInstance();
        ProgressIndicator taskIndicator = new EmptyProgressIndicator();
        GithubApiRequestExecutor executor = spy(GithubApiRequestExecutor.Factory.getInstance()
                .create(GithubServerPath.DEFAULT_SERVER, "test-token"));
        List<SubmittedReportInfo> reports = new ArrayList<>();

        doAnswer(invocation -> {
            ProgressIndicator requestIndicator = invocation.getArgument(0);
            return progressManager.runProcess((Computable<Object>) () -> {
                taskIndicator.cancel();
                assertTrue(requestIndicator.isCanceled());
                requestIndicator.checkCanceled();
                fail("The request must honor task cancellation");
                return null;
            }, requestIndicator);
        }).when(executor).execute(any(ProgressIndicator.class), any());

        assertThrows(ProcessCanceledException.class, () -> progressManager.runProcess(() ->
                classUnderTest.submitToGithub(new IdeaLoggingEvent("Test", new Throwable("Test error")),
                        null, executor, reports::add, taskIndicator), taskIndicator));
        assertEmpty(reports);
    }
}
