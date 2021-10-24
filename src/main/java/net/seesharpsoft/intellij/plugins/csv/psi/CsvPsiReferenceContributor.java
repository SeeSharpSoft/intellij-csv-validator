package net.seesharpsoft.intellij.plugins.csv.psi;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.impl.source.resolve.reference.ArbitraryPlaceUrlReferenceProvider;
import org.jetbrains.annotations.NotNull;

public class CsvPsiReferenceContributor extends PsiReferenceContributor {

    private static PsiReferenceProvider CSV_URL_REFERENCE_PROVIDER_SINGLETON = new ArbitraryPlaceUrlReferenceProvider();

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(PlatformPatterns.psiFile(CsvFile.class), CSV_URL_REFERENCE_PROVIDER_SINGLETON, PsiReferenceRegistrar.LOWER_PRIORITY);
    }
}
