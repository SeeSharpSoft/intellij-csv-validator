package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;

import java.util.regex.Pattern;

public final class CsvStorageHelper {
    public static final String CSV_STATE_STORAGE_FILE = "csv-editor.xml";

    public static final Key<String> RELATIVE_FILE_URL = Key.create("CSV_PLUGIN_RELATIVE_URL");

    public static String getRelativeFilePath(Project project, VirtualFile virtualFile) {
        if (project == null || virtualFile == null) {
            return null;
        }
        VirtualFile targetFile = virtualFile instanceof VirtualFileWindow ?
                ((VirtualFileWindow) virtualFile).getDelegate() :
                virtualFile;
        String filePath = targetFile.getUserData(RELATIVE_FILE_URL);
        if (filePath == null && project.getBasePath() != null) {
            String localFilePath = PathUtil.getLocalPath(targetFile);
            if (localFilePath == null) {
                return null;
            }
            String projectDir = PathUtil.getLocalPath(project.getBasePath());
            filePath = localFilePath.replaceFirst("^" + Pattern.quote(projectDir), "");
            targetFile.putUserData(RELATIVE_FILE_URL, filePath);
        }
        return filePath;
    }

    public static boolean csvFileExists(Project project, String fileName) {
        if (fileName == null) {
            return false;
        }
        String filePath = PathUtil.getLocalPath(fileName);
        if (filePath == null ||
                !CsvHelper.isCsvFile(PathUtil.getFileExtension(filePath))) {
            return false;
        }
        if (project != null && FileUtil.exists(PathUtil.getLocalPath(project.getBasePath()) + filePath)) {
            return true;
        }
        return FileUtil.exists(filePath);
    }

    private CsvStorageHelper() {
        // static
    }
}
