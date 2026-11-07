package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Ref;
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor;
import org.jetbrains.plugins.github.authentication.GHAccountsUtil;
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class CsvGithubAuthentication {

    // The replacement public APIs are absent in 252; use legacy entry points only on older platforms.
    private static final Method LOG_IN = findMethod(GHAccountsUtil.class, "logIn", Project.class);
    private static final Method ACCOUNT_EXECUTOR =
            findMethod(GithubApiRequestExecutor.Factory.class, "create", GithubAccount.class);

    private CsvGithubAuthentication() {
    }

    static GithubAccount logIn(Project project) {
        return onEdt(() -> {
            if (LOG_IN != null) {
                return GithubAccount.class.cast(invoke(LOG_IN, null, project));
            }
            Object authData = invoke(requiredMethod(GHAccountsUtil.class, "requestNewAccount", Project.class), null, project);
            return authData == null ? null
                    : GithubAccount.class.cast(invoke(requiredMethod(authData.getClass(), "getAccount"), authData));
        });
    }

    static GithubApiRequestExecutor createExecutor(GithubAccount account, Project project) {
        GithubApiRequestExecutor.Factory factory = GithubApiRequestExecutor.Factory.getInstance();
        if (ACCOUNT_EXECUTOR != null) {
            return GithubApiRequestExecutor.class.cast(invoke(ACCOUNT_EXECUTOR, factory, account));
        }
        String token = legacyToken(account, project);
        return token == null ? null : factory.create(account.getServer(), token);
    }

    static boolean reLogIn(GithubAccount account, Project project) {
        return onEdt(() -> Boolean.TRUE.equals(invoke(
                requiredMethod(GHAccountsUtil.class, "reLogIn", GithubAccount.class, Project.class), null, account, project)));
    }

    private static String legacyToken(GithubAccount account, Project project) {
        try {
            Class<?> tokenApi = GHAccountsUtil.class.getClassLoader()
                    .loadClass("org.jetbrains.plugins.github.util.GHCompatibilityUtil");
            return String.class.cast(invoke(requiredMethod(tokenApi, "getOrRequestToken", GithubAccount.class, Project.class),
                    null, account, project));
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("GitHub token API is unavailable", exception);
        }
    }

    private static <T> T onEdt(Computable<T> computation) {
        if (ApplicationManager.getApplication().isDispatchThread()) {
            return computation.compute();
        }
        Ref<T> result = new Ref<>();
        ApplicationManager.getApplication().invokeAndWait(() -> result.set(computation.compute()),
                ModalityState.defaultModalityState());
        return result.get();
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... parameters) {
        try {
            return type.getMethod(name, parameters);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Method requiredMethod(Class<?> type, String name, Class<?>... parameters) {
        Method method = findMethod(type, name, parameters);
        if (method == null) {
            throw new IllegalStateException("GitHub authentication API is unavailable: " + type.getName() + "." + name);
        }
        return method;
    }

    private static Object invoke(Method method, Object receiver, Object... arguments) {
        try {
            return method.invoke(receiver, arguments);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot access GitHub authentication API: " + method.getName(), exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("GitHub authentication failed", cause);
        }
    }
}
