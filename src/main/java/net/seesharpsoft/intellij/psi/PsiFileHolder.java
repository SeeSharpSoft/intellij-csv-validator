package net.seesharpsoft.intellij.psi;

import com.intellij.openapi.Disposable;
import com.intellij.psi.PsiFile;

public interface PsiFileHolder extends Disposable {
    PsiFile getPsiFile();

    void dispose();
}
