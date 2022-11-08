package net.seesharpsoft.intellij.plugins.csv.psi;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.impl.source.resolve.reference.ArbitraryPlaceUrlReferenceProvider;
import org.jetbrains.annotations.NotNull;

public class CsvPsiReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(PlatformPatterns.psiFile(CsvFile.class), new ArbitraryPlaceUrlReferenceProvider(), PsiReferenceRegistrar.LOWER_PRIORITY);
    }
}
