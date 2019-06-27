package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import org.jetbrains.annotations.NotNull;

public class CsvFileTypeFactory extends FileTypeFactory {
    @Override
    public void createFileTypes(@NotNull FileTypeConsumer fileTypeConsumer) {
        fileTypeConsumer.consume(CsvFileType.INSTANCE, String.join(FileTypeConsumer.EXTENSION_DELIMITER, new String[]{"csv"}));
    }
}