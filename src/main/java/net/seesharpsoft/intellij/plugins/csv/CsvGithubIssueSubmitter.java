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
import com.intellij.openapi.progress.util.ProgressWrapper;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.Strings;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.github.api.GithubApiRequest;
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor;
import org.jetbrains.plugins.github.api.GithubApiRequests;
import org.jetbrains.plugins.github.api.GithubServerPath;
import org.jetbrains.plugins.github.authentication.GHAccountsUtil;
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount;
import org.jetbrains.plugins.github.exceptions.GithubMissingTokenException;

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
    private static final String REPORT_ACTION_TEXT = "Report to 'CSV Editor' (Github)";

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
        return REPORT_ACTION_TEXT;
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
        GithubAccount existingAccount = GHAccountsUtil.getSingleOrDefaultAccount(project);
        GithubAccount account = existingAccount == null ? CsvGithubAuthentication.logIn(project) : existingAccount;
        if (account == null) return false;

        GithubApiRequestExecutor githubExecutor = CsvGithubAuthentication.createExecutor(account, project);
        if (githubExecutor == null) return false;

        Task submitTask = new Task.Backgroundable(project, REPORT_ACTION_TEXT) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    submitToGithub(event, additionalInfo, githubExecutor, consumer, indicator);
                } catch (CsvGithubSubmitException exception) {
                    if (!(exception.getCause() instanceof GithubMissingTokenException)) {
                        throw exception;
                    }
                    if (!CsvGithubAuthentication.reLogIn(account, project)) {
                        throw new ProcessCanceledException();
                    }
                    submitToGithub(event, additionalInfo, githubExecutor, consumer, indicator);
                }
            }
        };

        if (ApplicationManager.getApplication().isUnitTestMode()) {
            submitTask.run(new EmptyProgressIndicator());
        } else {
            ProgressManager.getInstance().run(submitTask);
        }

        return true;
    }

    protected void submitToGithub(IdeaLoggingEvent event,
                                String additionalInfo,
                                GithubApiRequestExecutor githubExecutor,
                                Consumer<? super SubmittedReportInfo> consumer,
                                ProgressIndicator progressIndicator) {
        try {
            String issueTitle = getIssueTitle(event);
            String issueDetails = getIssueDetails(event, additionalInfo);
            githubExecutor.execute(ProgressWrapper.wrap(progressIndicator), createNewIssue(issueTitle, issueDetails));
            SubmittedReportInfo.SubmissionStatus status = SubmittedReportInfo.SubmissionStatus.NEW_ISSUE;
            consumer.consume(new SubmittedReportInfo(status));
        } catch (IOException exc) {
            throw new CsvGithubSubmitException(exc);
        }
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
