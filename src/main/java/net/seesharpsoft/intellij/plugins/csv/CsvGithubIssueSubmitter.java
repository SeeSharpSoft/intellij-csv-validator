package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.Strings;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.github.api.*;
import org.jetbrains.plugins.github.api.data.GithubIssueState;
import org.jetbrains.plugins.github.api.data.GithubResponsePage;
import org.jetbrains.plugins.github.api.data.GithubSearchedIssue;
import org.jetbrains.plugins.github.api.data.request.GithubRequestPagination;
import org.jetbrains.plugins.github.authentication.GHAccountAuthData;
import org.jetbrains.plugins.github.authentication.GHAccountsUtil;
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount;
import org.jetbrains.plugins.github.util.GHCompatibilityUtil;

import java.awt.*;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CsvGithubIssueSubmitter extends ErrorReportSubmitter {

    private static final int FALLBACK_ISSUE_TITLE_LENGTH = 600;

    public static final String GIT_USER = "SeeSharpSoft";
    public static final String GIT_REPO = "intellij-csv-validator";
    public static final GHRepositoryPath GITHUB_FULL_PATH = new GHRepositoryPath(GIT_USER, GIT_REPO);

    private static ScheduledFuture<?> recentlySentReport = null;
    private static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private static class CsvGithubSubmitException extends RuntimeException {
        CsvGithubSubmitException(Throwable exception) {
            super(exception);
        }
    }

    @NotNull
    @Override
    public String getReportActionText() {
        return "Report to 'CSV Editor' (Github)";
    }

    @Override
    public synchronized boolean submit(IdeaLoggingEvent @NotNull [] events, @Nullable String additionalInfo, @NotNull Component parentComponent, @NotNull Consumer<? super SubmittedReportInfo> consumer) {
        if (reportWasRecentlySent()) return true;

        final DataContext dataContext = DataManager.getInstance().getDataContext(parentComponent);
        final Project project = CommonDataKeys.PROJECT.getData(dataContext);

        for (IdeaLoggingEvent event : events) {
            if (!submit(event, additionalInfo, project, consumer)) {
                return false;
            }
        }

        reportWasSent();
        return true;
    }

    protected void reportWasSent() {
        recentlySentReport = executorService.schedule(() -> { recentlySentReport = null; }, 15, TimeUnit.MINUTES);
    }

    protected boolean reportWasRecentlySent() {
        return recentlySentReport != null;
    }

    protected boolean submit(IdeaLoggingEvent event, String additionalInfo, Project project, Consumer<? super SubmittedReportInfo> consumer) {
        GithubAccount account = GHAccountsUtil.getSingleOrDefaultAccount(project);
        if (account == null) {
            GHAccountAuthData accountData = GHAccountsUtil.requestNewAccount(project);
            if (accountData == null) {
                return false;
            }
            account = accountData.getAccount();
        }
        String token = GHCompatibilityUtil.getOrRequestToken(account, project);
        if (token == null) return false;
        
        GithubApiRequestExecutor githubExecutor = GithubApiRequestExecutor.Factory.getInstance().create(account.getServer(), token);

        Task submitTask = new Task.Backgroundable(project, getReportActionText()) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                submitToGithub(event, additionalInfo, githubExecutor, consumer, indicator);
            }
        };

        if (ApplicationManager.getApplication().isUnitTestMode()) {
            submitTask.run(new EmptyProgressIndicator());
        } else {
            ProgressManager.getInstance().run(submitTask);
        }

        return true;
    }

    private void submitToGithub(IdeaLoggingEvent event,
                                String additionalInfo,
                                GithubApiRequestExecutor githubExecutor,
                                Consumer<? super SubmittedReportInfo> consumer,
                                ProgressIndicator progressIndicator) {
        try {
            SubmittedReportInfo.SubmissionStatus status;

            String issueTitle = getIssueTitle(event);
            String issueDetails = getIssueDetails(event, additionalInfo);
            String foundIssueId = searchExistingIssues(githubExecutor, issueTitle, progressIndicator);

            if (foundIssueId == null) {
                githubExecutor.execute(progressIndicator, createNewIssue(issueTitle, issueDetails));
                status = SubmittedReportInfo.SubmissionStatus.NEW_ISSUE;
            } else {
                if (!Strings.isEmpty(additionalInfo)) {
                    githubExecutor.execute(progressIndicator, updateExistingIssue(foundIssueId, issueDetails));
                }
                status = SubmittedReportInfo.SubmissionStatus.DUPLICATE;
            }
            consumer.consume(new SubmittedReportInfo(status));
        } catch (IOException exc) {
            throw new CsvGithubSubmitException(exc);
        }
    }

    protected GithubApiRequest<?> updateExistingIssue(String issueId, String content) throws IOException {
        return GithubApiRequests.Repos.Issues.Comments.create(
                GithubServerPath.DEFAULT_SERVER,
                GIT_USER,
                GIT_REPO,
                issueId,
                content
        );
    }

    protected GithubApiRequest<?> createNewIssue(String title, String content) throws IOException {
        return GithubApiRequests.Repos.Issues.create(
                GithubServerPath.DEFAULT_SERVER,
                GIT_USER,
                GIT_REPO,
                title,
                content,
                null,
                Collections.emptyList(),
                Collections.emptyList());
    }

    protected String searchExistingIssues(GithubApiRequestExecutor githubExecutor, String title, ProgressIndicator progressIndicator) throws IOException {
        String needle = title.replaceAll("\\s*(\\[.*?]|\\(.*?\\)|\\{.*?})\\s*", "");
        if (needle.length() > 250) {
            int endIndex = needle.substring(0, 250).lastIndexOf(" ");
            if (endIndex == -1) {
                endIndex = 250;
            }
            needle = needle.substring(0, endIndex);
        }
        GithubApiRequest<GithubResponsePage<GithubSearchedIssue>> existingIssueRequest =
                GithubApiRequests.Search.Issues.get(
                        GithubServerPath.DEFAULT_SERVER,
                        GITHUB_FULL_PATH,
                        GithubIssueState.open.name(),
                        null,
                        needle,
                        new GithubRequestPagination(1, 5)
                );

        GithubResponsePage<GithubSearchedIssue> foundIssuesPage = githubExecutor.execute(progressIndicator, existingIssueRequest);
        if (foundIssuesPage != null && !foundIssuesPage.getItems().isEmpty()) {
            for (GithubSearchedIssue foundIssue : foundIssuesPage.getItems()) {
                if (foundIssue.getTitle().equals(title)) {
                    return Long.toString(foundIssue.getNumber());
                }
            }
        }

        return null;
    }

    protected int getIssueTitleCutIndex(String throwableTextArg) {
        String throwableText = throwableTextArg.replaceAll("\r", "\n");
        int index = throwableText.indexOf("\n");
        if (index == -1) {
            index = throwableText.indexOf(" ", FALLBACK_ISSUE_TITLE_LENGTH);
        }
        return index == -1 ? Math.min(throwableText.length(), FALLBACK_ISSUE_TITLE_LENGTH) : index;
    }

    protected String getIssueTitle(IdeaLoggingEvent event) {
        String throwableText = event.getThrowableText();
        int index = getIssueTitleCutIndex(throwableText);
        String issueTitle = throwableText.substring(0, index).replaceAll("@[0-9a-fA-F]+", "");
        return "[Automated Report] " + issueTitle;
    }

    protected String getIssueDetails(IdeaLoggingEvent event, String additionalInfo) {
        return "Message\n---\n" +
                (!Strings.isEmpty(additionalInfo) ? additionalInfo : "<no message>") +
                "\n\n" +
                "Stacktrace\n---\n" +
                event.getThrowableText() +
                "\n\n" +
                "Plugin\n---\n" +
                getPluginDescriptor().getPluginClassLoader() +
                "\n\n" +
                "IDE\n---\n" +
                ApplicationInfo.getInstance().getVersionName() +
                " (" +
                ApplicationInfo.getInstance().getBuild() +
                ")";
    }

    @Override
    public String getPrivacyNoticeText() {
        return "An automated issue report contains the provided message, " +
                "the stacktrace, " +
                "the plugin class loader info (" +
                getPluginDescriptor().getPluginClassLoader() +
                "), the used IDE version name (" +
                ApplicationInfo.getInstance().getVersionName() +
                ") and build number (" +
                ApplicationInfo.getInstance().getBuild() +
                "). " +
                "By sending the report I agree to the information be used for resolving this and further related issues. " +
                "All provided information will be publicly available at " +
                String.format("https://%s/%s/%s/issues", GithubServerPath.DEFAULT_SERVER, GIT_USER, GIT_REPO);
    }
}
