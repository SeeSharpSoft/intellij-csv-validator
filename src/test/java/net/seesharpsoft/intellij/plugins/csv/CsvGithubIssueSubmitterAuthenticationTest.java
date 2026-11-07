package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.RunAll;
import org.jetbrains.plugins.github.api.GithubApiRequest;
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor;
import org.jetbrains.plugins.github.api.GithubServerPath;
import org.jetbrains.plugins.github.authentication.GHAccountsUtil;
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount;
import org.jetbrains.plugins.github.exceptions.GithubMissingTokenException;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CsvGithubIssueSubmitterAuthenticationTest extends CsvBasePlatformTestCase {

    private final CsvGithubIssueSubmitter submitter = new CsvGithubIssueSubmitter() {
        @Override
        protected String getIssueDetails(IdeaLoggingEvent event, String additionalInfo) {
            return "Test report details";
        }
    };
    private final List<SubmittedReportInfo> reports = new ArrayList<>();
    private final List<GithubApiRequest<?>> requests = new ArrayList<>();
    private GithubAccount account;
    private GithubApiRequestExecutor executor;
    private GithubApiRequestExecutor.Factory factory;
    private MockedStatic<GHAccountsUtil> accounts;
    private MockedStatic<GithubApiRequestExecutor.Factory> factories;
    private MockedStatic<?> legacyTokens;
    private Method legacyTokenMethod;
    private Method accountExecutorMethod;
    private Method loginMethod;
    private Method reLoginMethod;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        account = mock(GithubAccount.class);
        when(account.getServer()).thenReturn(GithubServerPath.DEFAULT_SERVER);
        executor = spy(GithubApiRequestExecutor.Factory.getInstance().create(GithubServerPath.DEFAULT_SERVER, "test-token"));
        doAnswer(invocation -> {
            GithubApiRequest<?> request = invocation.getArgument(1);
            requests.add(request);
            return null;
        }).when(executor).execute(any(ProgressIndicator.class), any());

        factory = mock(GithubApiRequestExecutor.Factory.class);
        when(factory.create(GithubServerPath.DEFAULT_SERVER, "test-token")).thenReturn(executor);
        accountExecutorMethod = findMethod(GithubApiRequestExecutor.Factory.class, "create", GithubAccount.class);
        if (accountExecutorMethod != null) {
            when(accountExecutorMethod.invoke(factory, account)).thenReturn(executor);
        }
        loginMethod = findMethod(GHAccountsUtil.class, "logIn", Project.class);
        reLoginMethod = findMethod(GHAccountsUtil.class, "reLogIn", GithubAccount.class, Project.class);
        if (loginMethod == null) {
            loginMethod = GHAccountsUtil.class.getMethod("requestNewAccount", Project.class);
        }
        Class<?> legacyTokenApi = Class.forName("org.jetbrains.plugins.github.util.GHCompatibilityUtil");
        legacyTokenMethod = legacyTokenApi.getMethod("getOrRequestToken", GithubAccount.class, Project.class);
        legacyTokens = mockStatic(legacyTokenApi);
        legacyTokens.when(() -> legacyTokenMethod.invoke(null, account, getProject())).thenReturn("test-token");
        accounts = mockStatic(GHAccountsUtil.class);
        factories = mockStatic(GithubApiRequestExecutor.Factory.class);
        factories.when(GithubApiRequestExecutor.Factory::getInstance).thenReturn(factory);
    }

    @Override
    protected void tearDown() throws Exception {
        RunAll.runAll(
                () -> {
                    if (factories != null) factories.close();
                },
                () -> {
                    if (accounts != null) accounts.close();
                },
                () -> {
                    if (legacyTokens != null) legacyTokens.close();
                },
                super::tearDown);
    }

    public void testExistingAccountUsesPublicExecutorWithoutLogin() throws Exception {
        selectAccount();

        assertTrue(submit());

        accounts.verify(() -> loginMethod.invoke(null, getProject()), never());
        if (accountExecutorMethod != null) {
            accountExecutorMethod.invoke(verify(factory), account);
            legacyTokens.verifyNoInteractions();
        } else {
            verify(factory).create(GithubServerPath.DEFAULT_SERVER, "test-token");
        }
        assertCompleted(1);
    }

    public void testNewAccountUsesPublicLoginOnTheEdt() throws Exception {
        Object loginResult = loginMethod.getReturnType() == GithubAccount.class ? account
                : loginMethod.getReturnType().getConstructor(GithubAccount.class, String.class, String.class)
                .newInstance(account, "test-user", "test-token");
        accounts.when(() -> loginMethod.invoke(null, getProject())).thenAnswer(invocation -> {
            assertTrue(ApplicationManager.getApplication().isDispatchThread());
            return loginResult;
        });

        assertTrue(submit());

        assertCompleted(1);
    }

    public void testCancelledLoginDoesNotSendAReport() {
        assertFalse(submit());
        assertEmpty(requests);
        assertEmpty(reports);
        verifyNoInteractions(factory);
    }

    public void testCancelledLegacyTokenLoginDoesNotSendAReport() {
        if (accountExecutorMethod != null) return;
        selectAccount();
        legacyTokens.when(() -> legacyTokenMethod.invoke(null, account, getProject())).thenReturn(null);

        assertFalse(submit());

        assertEmpty(requests);
        assertEmpty(reports);
    }

    public void testLoginCancellationPropagatesWithoutTryingAnotherApi() {
        ProcessCanceledException cancellation = new ProcessCanceledException();
        accounts.when(() -> loginMethod.invoke(null, getProject())).thenThrow(cancellation);

        assertSame(cancellation, org.junit.Assert.assertThrows(ProcessCanceledException.class, this::submit));

        assertEmpty(requests);
        assertEmpty(reports);
    }

    public void testLoginFailuresPropagateWithoutTryingAnotherApi() {
        IllegalStateException failure = new IllegalStateException("Test login failed");
        accounts.when(() -> loginMethod.invoke(null, getProject())).thenThrow(failure);

        assertSame(failure, org.junit.Assert.assertThrows(IllegalStateException.class, this::submit));

        assertEmpty(requests);
        assertEmpty(reports);
    }

    public void testMissingTokenPromptsForLoginAndRetries() throws Exception {
        if (accountExecutorMethod == null) return;
        assertNotNull(reLoginMethod);
        selectAccount();
        stubSuccessfulRequestsAfterMissingToken();
        accounts.when(() -> reLoginMethod.invoke(null, account, getProject())).thenAnswer(invocation -> {
            assertTrue(ApplicationManager.getApplication().isDispatchThread());
            return true;
        });

        assertTrue(submit());

        accounts.verify(() -> reLoginMethod.invoke(null, account, getProject()), times(1));
        assertCompleted(2);
    }

    public void testCancelledTokenLoginDoesNotSendAReport() throws Exception {
        if (accountExecutorMethod == null) return;
        assertNotNull(reLoginMethod);
        selectAccount();
        doThrow(new GithubMissingTokenException(account)).when(executor).execute(any(ProgressIndicator.class), any());

        assertThrows(ProcessCanceledException.class, this::submit);

        assertEmpty(requests);
        assertEmpty(reports);
        accounts.verify(() -> reLoginMethod.invoke(null, account, getProject()), times(1));
    }

    public void testTokenStillMissingAfterLoginDoesNotRetryAgain() throws Exception {
        if (accountExecutorMethod == null) return;
        assertNotNull(reLoginMethod);
        selectAccount();
        GithubMissingTokenException failure = new GithubMissingTokenException(account);
        doThrow(failure).when(executor).execute(any(ProgressIndicator.class), any());
        accounts.when(() -> reLoginMethod.invoke(null, account, getProject())).thenReturn(true);

        RuntimeException exception = org.junit.Assert.assertThrows(RuntimeException.class, this::submit);

        assertSame(failure, exception.getCause());
        assertEmpty(reports);
        accounts.verify(() -> reLoginMethod.invoke(null, account, getProject()), times(1));
        verify(executor, times(2)).execute(any(ProgressIndicator.class), any());
    }

    public void testOtherRequestFailuresDoNotPromptForLogin() throws Exception {
        selectAccount();
        IOException failure = new IOException("Test request failed");
        doThrow(failure).when(executor).execute(any(ProgressIndicator.class), any());

        RuntimeException exception = org.junit.Assert.assertThrows(RuntimeException.class, this::submit);

        assertSame(failure, exception.getCause());
        assertEmpty(reports);
        if (reLoginMethod != null) {
            accounts.verify(() -> reLoginMethod.invoke(null, account, getProject()), never());
        }
    }

    private void stubSuccessfulRequestsAfterMissingToken() throws IOException {
        doAnswer(invocation -> {
            if (requests.isEmpty()) {
                requests.add(invocation.getArgument(1));
                throw new GithubMissingTokenException(account);
            }
            GithubApiRequest<?> request = invocation.getArgument(1);
            requests.add(request);
            return null;
        }).when(executor).execute(any(ProgressIndicator.class), any());
    }

    private void selectAccount() {
        accounts.when(() -> GHAccountsUtil.getSingleOrDefaultAccount(getProject())).thenReturn(account);
    }

    private boolean submit() {
        return submitter.submit(new IdeaLoggingEvent("Test", new Throwable("Test error")),
                null, getProject(), reports::add);
    }

    private void assertCompleted(int expectedRequests) {
        assertEquals(expectedRequests, requests.size());
        assertEquals(1, reports.size());
        assertEquals(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE, reports.getFirst().getStatus());
        for (GithubApiRequest<?> request : requests) {
            assertInstanceOf(request, GithubApiRequest.Post.class);
            assertEquals("https://api.github.com/repos/SeeSharpSoft/intellij-csv-validator/issues", request.getUrl());
        }
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... parameters) {
        try {
            return type.getMethod(name, parameters);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }
}
