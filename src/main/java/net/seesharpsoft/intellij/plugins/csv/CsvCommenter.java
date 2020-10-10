package net.seesharpsoft.intellij.plugins.csv;

import com.intellij.lang.Commenter;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvEditorSettings;
import org.jetbrains.annotations.Nullable;

public class CsvCommenter implements Commenter {

    @Nullable
    @Override
    public String getLineCommentPrefix() {
        String commentIndicator = CsvEditorSettings.getInstance().getCommentIndicator();
        return commentIndicator == null || commentIndicator.isEmpty() ? null : commentIndicator + " ";
    }

    @Nullable
    @Override
    public String getBlockCommentPrefix() {
        return null;
    }

    @Nullable
    @Override
    public String getBlockCommentSuffix() {
        return null;
    }

    @Nullable
    @Override
    public String getCommentedBlockCommentPrefix() {
        return null;
    }

    @Nullable
    @Override
    public String getCommentedBlockCommentSuffix() {
        return null;
    }
}
