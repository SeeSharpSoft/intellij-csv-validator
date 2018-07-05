package net.seesharpsoft.intellij.plugins.csv.inspection;

import com.intellij.codeInspection.InspectionProfileEntry;
import com.intellij.codeInspection.InspectionSuppressor;
import com.intellij.codeInspection.ProblematicWhitespaceInspection;
import com.intellij.codeInspection.SuppressQuickFix;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class CsvInspectionSuppressor implements InspectionSuppressor {

    private static final Set<String> GENERAL_SUPPRESSED_INSPECTIONS;

    static {
        GENERAL_SUPPRESSED_INSPECTIONS = new HashSet<>();
        GENERAL_SUPPRESSED_INSPECTIONS.add(InspectionProfileEntry.getShortName(ProblematicWhitespaceInspection.class.getSimpleName()));
    }

    @Override
    public boolean isSuppressedFor(@NotNull PsiElement psiElement, @NotNull String s) {
        return GENERAL_SUPPRESSED_INSPECTIONS.contains(s);
    }

    @NotNull
    @Override
    public SuppressQuickFix[] getSuppressActions(@Nullable PsiElement psiElement, @NotNull String s) {
        return new SuppressQuickFix[0];
    }
}
