package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.impl.FileTypeOverrider;
import com.intellij.openapi.vfs.VirtualFile;
import net.seesharpsoft.intellij.plugins.psv.PsvFileType;
import net.seesharpsoft.intellij.plugins.tsv.TsvFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CsvFileTypeOverrider implements FileTypeOverrider {
    @Nullable
    @Override
    public FileType getOverriddenFileType(@NotNull VirtualFile file) {
        if (file != null) {
            String extension = file.getExtension();
            if (extension != null) {
                switch (extension.toLowerCase()) {
                    case "csv":
                        return CsvFileType.INSTANCE;
                    case "tsv":
                    case "tab":
                        return TsvFileType.INSTANCE;
                    case "psv":
                        return PsvFileType.INSTANCE;
                    default:
                        return null;
                }
            }
        }
        return null;
    }
}
