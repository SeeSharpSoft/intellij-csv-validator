package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;

import java.util.regex.Pattern;

public final class CsvStorageHelper {
    public static final String CSV_STATE_STORAGE_FILE = "csv-plugin.xml";

    public static final Key<String> RELATIVE_FILE_URL = Key.create("CSV_PLUGIN_RELATIVE_URL");

    public static String getRelativeFileUrl(Project project, VirtualFile virtualFile) {
        if (project == null || virtualFile == null) {
            return null;
        }
        String url = virtualFile.getUserData(RELATIVE_FILE_URL);
        if (url == null) {
            String projectDir = PathUtil.getLocalPath(project.getBasePath());
            url = PathUtil.getLocalPath(virtualFile.getPath())
                    .replaceFirst("^" + Pattern.quote(projectDir), "");
            virtualFile.putUserData(RELATIVE_FILE_URL, url);
        }
        return url;
    }

    private CsvStorageHelper() {
        // static
    }
}
