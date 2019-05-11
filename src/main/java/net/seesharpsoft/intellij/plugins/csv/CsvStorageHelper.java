package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiFile;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public final class CsvStorageHelper {
    public static final String CSV_STATE_STORAGE_FILE = "csv-plugin.xml";

    public static final Key<String> RELATIVE_FILE_URL = Key.create("CSV_PLUGIN_RELATIVE_URL");

    public static String getRelativeFileUrl(@NotNull PsiFile psiFile) {
        String url = psiFile.getUserData(RELATIVE_FILE_URL);
        if (url == null) {
            String projectDir = PathUtil.getLocalPath(psiFile.getProject().getBasePath());
            url = PathUtil.getLocalPath(psiFile.getOriginalFile().getVirtualFile().getPath())
                    .replaceFirst("^" + Pattern.quote(projectDir), "");
            psiFile.putUserData(RELATIVE_FILE_URL, url);
        }
        return url;
    }

    private CsvStorageHelper() {
        // static
    }
}
