package net.seesharpsoft.intellij.plugins.tsv;

import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import org.jetbrains.annotations.NotNull;

public class TsvFileTypeFactory extends FileTypeFactory {
    @Override
    public void createFileTypes(@NotNull FileTypeConsumer fileTypeConsumer) {
        fileTypeConsumer.consume(TsvFileType.INSTANCE, String.join(FileTypeConsumer.EXTENSION_DELIMITER, new String[] {"tsv", "tab"}));
    }
}