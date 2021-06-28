package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.github.api.*;
import org.jetbrains.plugins.github.api.data.*;
import org.jetbrains.plugins.github.api.data.request.GithubRequestPagination;
import org.jetbrains.plugins.github.authentication.GithubAuthenticationManager;
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount;

import java.awt.*;
import java.io.IOException;
import java.util.Collections;

public class CsvGithubIssueSubmitter extends ErrorReportSubmitter {

    public static final String GIT_USER = "SeeSharpSoft";
    public static final String GIT_REPO = "intellij-csv-validator";
    public static final GHRepositoryPath GITHUB_FULL_PATH = new GHRepositoryPath(GIT_USER, GIT_REPO);

    private static class CsvGithubSubmitException extends RuntimeException {
        CsvGithubSubmitException(Throwable exception) {
            super(exception);
        }
    }

    @NotNull
    @Override
    public String getReportActionText() {
        return "Report to 'CSV Plugin' (Github)";
    }

    @Override
    public boolean submit(@NotNull IdeaLoggingEvent[] events, @Nullable String additionalInfo, @NotNull Component parentComponent, @NotNull Consumer<SubmittedReportInfo> consumer) {
        final DataContext dataContext = DataManager.getInstance().getDataContext(parentComponent);
        final Project project = CommonDataKeys.PROJECT.getData(dataContext);

        for (IdeaLoggingEvent event : events) {
            if (!submit(event, additionalInfo, project, consumer)) {
                return false;
            }
        }
        return true;
    }

    protected boolean submit(IdeaLoggingEvent event, String additionalInfo, Project project, Consumer<SubmittedReportInfo> consumer) {
        GithubAuthenticationManager githubAuthManager = GithubAuthenticationManager.getInstance();
        if (!githubAuthManager.ensureHasAccounts(project)) {
            return false;
        }
        GithubAccount githubAccount = githubAuthManager.getSingleOrDefaultAccount(project);
        GithubApiRequestExecutor githubExecutor = GithubApiRequestExecutorManager.getInstance().getExecutor(githubAccount, project);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
            ProgressManager.getInstance().runProcess(() -> {
                submitToGithub(event, additionalInfo, githubExecutor, consumer);
            }, progressIndicator);
        });

        return true;
    }

    private void submitToGithub(IdeaLoggingEvent event, String additionalInfo, GithubApiRequestExecutor githubExecutor, Consumer<SubmittedReportInfo> consumer) {
        try {
            SubmittedReportInfo.SubmissionStatus status;

            String issueTitle = getIssueTitle(event);
            String issueDetails = getIssueDetails(event, additionalInfo);
            String foundIssueId = searchExistingIssues(githubExecutor, issueTitle);

            if (foundIssueId == null) {
                createNewIssue(githubExecutor, issueTitle, issueDetails);
                status = SubmittedReportInfo.SubmissionStatus.NEW_ISSUE;
            } else {
                updateExistingIssue(githubExecutor, foundIssueId, issueDetails);
                status = SubmittedReportInfo.SubmissionStatus.DUPLICATE;
            }
            consumer.consume(new SubmittedReportInfo(status));
        } catch (IOException exc) {
            throw new CsvGithubSubmitException(exc);
        }
    }

    protected void updateExistingIssue(GithubApiRequestExecutor githubExecutor, String issueId, String content) throws IOException {
        GithubApiRequest.Post createIssueCommentRequest =
                GithubApiRequests.Repos.Issues.Comments.create(
                        GithubServerPath.DEFAULT_SERVER,
                        GIT_USER,
                        GIT_REPO,
                        issueId,
                        content
                );

        githubExecutor.execute(createIssueCommentRequest);
    }

    protected void createNewIssue(GithubApiRequestExecutor githubExecutor, String title, String content) throws IOException {
        GithubApiRequest.Post<GithubIssue> createIssueRequest =
                GithubApiRequests.Repos.Issues.create(
                        GithubServerPath.DEFAULT_SERVER,
                        GIT_USER,
                        GIT_REPO,
                        title,
                        content,
                        null,
                        Collections.emptyList(),
                        Collections.emptyList());

        githubExecutor.execute(createIssueRequest);
    }

    protected String searchExistingIssues(GithubApiRequestExecutor githubExecutor, String needle) throws IOException {
        GithubApiRequest<GithubResponsePage<GithubSearchedIssue>> existingIssueRequest =
                GithubApiRequests.Search.Issues.get(
                        GithubServerPath.DEFAULT_SERVER,
                        GITHUB_FULL_PATH,
                        GithubIssueState.open.name(),
                        null,
                        needle,
                        new GithubRequestPagination(1, 1)
                );

        String issueId = null;
        GithubResponsePage<GithubSearchedIssue> foundIssuesPage = githubExecutor.execute(existingIssueRequest);
        if (foundIssuesPage != null && !foundIssuesPage.getItems().isEmpty()) {
            GithubSearchedIssue foundIssue = foundIssuesPage.getItems().get(0);
            if (foundIssue.getTitle().equals(needle)) {
                issueId = Long.toString(foundIssue.getNumber());
            }
        }

        return issueId;
    }

    protected String getIssueTitle(IdeaLoggingEvent event) {
        String throwableText = event.getThrowableText();
        int index = Math.min(throwableText.indexOf("\r"), throwableText.indexOf("\n"));
        return "[Automated Report] " + event.getThrowableText().substring(0, index);
    }

    protected String getIssueDetails(IdeaLoggingEvent event, String additionalInfo) {
        StringBuilder builder = new StringBuilder()
                .append("Message\n---\n")
                .append(additionalInfo != null && !additionalInfo.isEmpty() ? additionalInfo : "<no message>")
                .append("\n\n")
                .append("Stacktrace\n---\n")
                .append(event.getThrowableText())
                .append("\n\n")
                .append("Plugin\n---\n")
                .append(getPluginDescriptor().getPluginClassLoader().toString())
                .append("\n\n")
                .append("IDE\n---\n")
                .append(ApplicationInfo.getInstance().getVersionName())
                .append(" (")
                .append(ApplicationInfo.getInstance().getBuild().toString())
                .append(")");

        return builder.toString();
    }

    @Override
    public String getPrivacyNoticeText() {
        StringBuilder builder = new StringBuilder()
                .append("An automated issue report contains the provided message, ")
                .append("the stacktrace, ")
                .append("the plugin class loader info (")
                .append(getPluginDescriptor().getPluginClassLoader().toString())
                .append("), the used IDE version name (")
                .append(ApplicationInfo.getInstance().getVersionName())
                .append(") and build number (")
                .append(ApplicationInfo.getInstance().getBuild().toString())
                .append("). ")
                .append("By sending the report I agree to the information be used for resolving this and further related issues. ")
                .append("Github Issue Link: ")
                .append(String.format("https://%s/%s/%s/issues", GithubServerPath.DEFAULT_SERVER, GIT_USER, GIT_REPO));
        return builder.toString();
    }
}
