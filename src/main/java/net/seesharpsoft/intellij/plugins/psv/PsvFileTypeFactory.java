package net.seesharpsoft.intellij.plugins.psv;

import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import org.jetbrains.annotations.NotNull;

public class PsvFileTypeFactory extends FileTypeFactory {
    @Override
    public void createFileTypes(@NotNull FileTypeConsumer fileTypeConsumer) {
        fileTypeConsumer.consume(PsvFileType.INSTANCE, String.join(FileTypeConsumer.EXTENSION_DELIMITER, new String[]{"psv"}));
    }
}