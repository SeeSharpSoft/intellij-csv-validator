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
import org.jetbrains.plugins.github.api.data.GithubResponsePage;
import org.jetbrains.plugins.github.api.data.GithubSearchedIssue;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CsvGithubIssueSubmitterProgressTest extends CsvBasePlatformTestCase {

    private static final String TITLE = "[Automated Report] java.lang.Throwable: Test error";

    private final CsvGithubIssueSubmitter classUnderTest = new CsvGithubIssueSubmitter() {
        @Override
        protected String getIssueDetails(IdeaLoggingEvent event, String additionalInfo) {
            return "Test report details";
        }
    };

    public void testNewIssueKeepsTaskIndicatorRunning() throws Exception {
        assertSubmission(false, null, SubmittedReportInfo.SubmissionStatus.NEW_ISSUE, 2);
    }

    public void testDuplicateCommentKeepsTaskIndicatorRunning() throws Exception {
        assertSubmission(true, "Additional details", SubmittedReportInfo.SubmissionStatus.DUPLICATE, 2);
    }

    public void testDuplicateWithoutDetailsOnlySearches() throws Exception {
        assertSubmission(true, null, SubmittedReportInfo.SubmissionStatus.DUPLICATE, 1);
    }

    private void assertSubmission(boolean duplicate, String additionalInfo,
                                  SubmittedReportInfo.SubmissionStatus expectedStatus, int expectedRequests) throws Exception {
        ProgressManager progressManager = ProgressManager.getInstance();
        ProgressIndicator taskIndicator = new ProgressIndicatorBase();
        GithubApiRequestExecutor executor = mock(GithubApiRequestExecutor.WithTokenAuth.class);
        List<ProgressIndicator> requestIndicators = new ArrayList<>();
        List<GithubApiRequest<?>> requests = new ArrayList<>();
        List<SubmittedReportInfo> reports = new ArrayList<>();

        GithubSearchedIssue foundIssue = mock(GithubSearchedIssue.class);
        when(foundIssue.getTitle()).thenReturn(TITLE);
        when(foundIssue.getNumber()).thenReturn(1080L);
        GithubResponsePage<GithubSearchedIssue> searchResult = new GithubResponsePage<>(
                duplicate ? List.of(foundIssue) : List.of(), null, null, null, null);

        when(executor.execute(any(), any())).thenAnswer(invocation -> {
            ProgressIndicator requestIndicator = invocation.getArgument(0);
            assertNotSame(taskIndicator, requestIndicator);
            assertFalse(requestIndicator.isRunning());
            assertTrue(taskIndicator.isRunning());
            assertSame(taskIndicator, progressManager.getProgressIndicator());
            requestIndicators.add(requestIndicator);
            requests.add(invocation.getArgument(1));

            // Reproduce GithubApiHelperRequestExecutor's nested progress lifecycle without HTTP.
            Object result = progressManager.runProcess((Computable<Object>) () -> {
                assertSame(requestIndicator, progressManager.getProgressIndicator());
                assertTrue(requestIndicator.isRunning());
                requestIndicator.setText("Sending report");
                assertEquals("Sending report", taskIndicator.getText());
                return requests.size() == 1 ? searchResult : null;
            }, requestIndicator);

            assertFalse(requestIndicator.isRunning());
            assertTrue(taskIndicator.isRunning());
            assertSame(taskIndicator, progressManager.getProgressIndicator());
            return result;
        });

        progressManager.runProcess(() -> {
            classUnderTest.submitToGithub(new IdeaLoggingEvent("Test", new Throwable("Test error")),
                    additionalInfo, executor, reports::add, taskIndicator);
            assertTrue(taskIndicator.isRunning());
        }, taskIndicator);

        assertFalse(taskIndicator.isRunning());
        assertEquals(expectedRequests, requests.size());
        assertEquals(1, reports.size());
        assertEquals(expectedStatus, reports.get(0).getStatus());
        assertTrue(requests.get(0).getUrl().contains("/search/issues"));
        if (expectedRequests == 2) {
            assertNotSame(requestIndicators.get(0), requestIndicators.get(1));
            assertTrue(requests.get(1).getUrl().endsWith(duplicate ? "/issues/1080/comments" : "/issues"));
        }
    }

    public void testTaskCancellationReachesRequestIndicator() throws Exception {
        ProgressManager progressManager = ProgressManager.getInstance();
        ProgressIndicator taskIndicator = new EmptyProgressIndicator();
        GithubApiRequestExecutor executor = mock(GithubApiRequestExecutor.WithTokenAuth.class);
        List<SubmittedReportInfo> reports = new ArrayList<>();

        when(executor.execute(any(), any())).thenAnswer(invocation -> {
            ProgressIndicator requestIndicator = invocation.getArgument(0);
            return progressManager.runProcess((Computable<Object>) () -> {
                taskIndicator.cancel();
                assertTrue(requestIndicator.isCanceled());
                requestIndicator.checkCanceled();
                fail("The request must honor task cancellation");
                return null;
            }, requestIndicator);
        });

        assertThrows(ProcessCanceledException.class, () -> progressManager.runProcess(() ->
                classUnderTest.submitToGithub(new IdeaLoggingEvent("Test", new Throwable("Test error")),
                        null, executor, reports::add, taskIndicator), taskIndicator));
        assertEmpty(reports);
    }
}
