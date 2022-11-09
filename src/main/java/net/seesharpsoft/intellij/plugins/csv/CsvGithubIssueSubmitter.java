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
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.github.api.*;
import org.jetbrains.plugins.github.api.data.GithubIssueState;
import org.jetbrains.plugins.github.api.data.GithubResponsePage;
import org.jetbrains.plugins.github.api.data.GithubSearchedIssue;
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
        return "Report to 'CSV Editor' (Github)";
    }

    @Override
    public boolean submit(IdeaLoggingEvent @NotNull [] events, @Nullable String additionalInfo, @NotNull Component parentComponent, @NotNull Consumer<? super SubmittedReportInfo> consumer) {
        final DataContext dataContext = DataManager.getInstance().getDataContext(parentComponent);
        final Project project = CommonDataKeys.PROJECT.getData(dataContext);

        for (IdeaLoggingEvent event : events) {
            if (!submit(event, additionalInfo, project, consumer)) {
                return false;
            }
        }
        return true;
    }

    protected boolean submit(IdeaLoggingEvent event, String additionalInfo, Project project, Consumer<? super SubmittedReportInfo> consumer) {
        GithubAuthenticationManager githubAuthManager = GithubAuthenticationManager.getInstance();
        if (!githubAuthManager.ensureHasAccounts(project)) {
            return false;
        }
        GithubAccount githubAccount = githubAuthManager.getSingleOrDefaultAccount(project);
        assert githubAccount != null;
        // the cast shouldn't be needed due to inheritance, but Java complains for some reason in 2022.1
        GithubApiRequestExecutor githubExecutor = GithubApiRequestExecutor.class.cast(GithubApiRequestExecutorManager.getInstance().getExecutor(githubAccount, project));

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
                githubExecutor.execute(progressIndicator, updateExistingIssue(foundIssueId, issueDetails));
                status = SubmittedReportInfo.SubmissionStatus.DUPLICATE;
            }
            consumer.consume(new SubmittedReportInfo(status));
        } catch (IOException exc) {
            throw new CsvGithubSubmitException(exc);
        }
    }

    protected GithubApiRequest updateExistingIssue(String issueId, String content) throws IOException {
        return GithubApiRequests.Repos.Issues.Comments.create(
                GithubServerPath.DEFAULT_SERVER,
                GIT_USER,
                GIT_REPO,
                issueId,
                content
        );
    }

    protected GithubApiRequest createNewIssue(String title, String content) throws IOException {
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

    protected String searchExistingIssues(GithubApiRequestExecutor githubExecutor, String needleArg, ProgressIndicator progressIndicator) throws IOException {
        String needle = needleArg;
        if (needle.length() > 255) {
            needle = needle.substring(0, needle.substring(0, 255).lastIndexOf(" "));
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
                if (foundIssue.getTitle().equals(needleArg)) {
                    return Long.toString(foundIssue.getNumber());
                }
            }
        }

        return null;
    }

    protected String getIssueTitle(IdeaLoggingEvent event) {
        String throwableText = event.getThrowableText();
        int index = Math.min(throwableText.indexOf("\r"), throwableText.indexOf("\n"));
        return "[Automated Report] " + event.getThrowableText().substring(0, index);
    }

    protected String getIssueDetails(IdeaLoggingEvent event, String additionalInfo) {
        return "Message\n---\n" +
                (additionalInfo != null && !additionalInfo.isEmpty() ? additionalInfo : "<no message>") +
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
