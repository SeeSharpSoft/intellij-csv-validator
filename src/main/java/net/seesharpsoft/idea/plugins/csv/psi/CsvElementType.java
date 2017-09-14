package net.seesharpsoft.idea.plugins.csv.psi;

import com.intellij.psi.tree.IElementType;
import net.seesharpsoft.idea.plugins.csv.CsvLanguage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class CsvElementType extends IElementType {
  public CsvElementType(@NotNull @NonNls String debugName) {
    super(debugName, CsvLanguage.INSTANCE);
  }
}