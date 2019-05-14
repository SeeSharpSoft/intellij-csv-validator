package net.seesharpsoft.intellij.plugins.csv.inspection;

import com.intellij.codeInspection.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.IncorrectOperationException;
import net.seesharpsoft.intellij.plugins.csv.CsvHelper;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import net.seesharpsoft.intellij.plugins.csv.intention.CsvIntentionHelper;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import net.seesharpsoft.intellij.plugins.csv.settings.CsvCodeStyleSettings;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author max
 */
public class CsvValidationInspection extends LocalInspectionTool {
    private static final Logger LOG = Logger.getInstance("#net.seesharpsoft.intellij.plugins.csv.inspection.CsvSyntaxInspection");

    private final LocalQuickFix fixUnescapedSequence = new UnescapedSequenceFix();
    private final LocalQuickFix fixSeparatorMissing = new SeparatorMissingFix();
    private final LocalQuickFix fixClosingQuoteMissing = new ClosingQuoteMissingFix();

    @NonNls
    private static final String UNESCAPED_SEQUENCE = "Unescaped sequence";
    @NonNls
    private static final String SEPARATOR_MISSING = "Separator missing";
    @NonNls
    private static final String CLOSING_QUOTE_MISSING = "Quote missing";

    @NotNull
    public String getDisplayName() {
        return "Propose possible fixes";
    }

    @Nullable
    public String getStaticDescription() {
        return "Propose possible fixes to invalid syntax in CSV files.";
    }

    @NotNull
    public String getGroupDisplayName() {
        return "CSV";
    }

    @NotNull
    public String getShortName() {
        return "CsvValidation";
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (element == null || !holder.getFile().getLanguage().isKindOf(CsvLanguage.INSTANCE)) {
                    return;
                }

                IElementType elementType = CsvHelper.getElementType(element);
                PsiElement firstChild = element.getFirstChild();
                PsiElement nextSibling = element.getNextSibling();
                if (elementType == TokenType.ERROR_ELEMENT && CsvHelper.getElementType(firstChild) == TokenType.BAD_CHARACTER) {
                    CsvValidationInspection.this.registerError(holder, element, UNESCAPED_SEQUENCE, fixUnescapedSequence);
                    if (!"\"".equals(firstChild.getText())) {
                        CsvValidationInspection.this.registerError(holder, element, SEPARATOR_MISSING, fixSeparatorMissing);
                    }
                } else if ((elementType == CsvTypes.TEXT || elementType == CsvTypes.ESCAPED_TEXT) &&
                        CsvHelper.getElementType(nextSibling) == TokenType.ERROR_ELEMENT &&
                        nextSibling.getFirstChild() == null) {
                    CsvValidationInspection.this.registerError(holder, element, CLOSING_QUOTE_MISSING, fixClosingQuoteMissing);
                }
            }
        };
    }

    private boolean registerError(@NotNull final ProblemsHolder holder, @NotNull PsiElement element, @NotNull String descriptionTemplate, @Nullable LocalQuickFix fix) {
        if (element != null && this.isSuppressedFor(element)) {
            return false;
        }

        holder.registerProblem(element, descriptionTemplate, fix);
        return true;
    }

    private abstract static class CsvLocalQuickFix implements LocalQuickFix {
        @NotNull
        public String getName() {
            return this.getFamilyName();
        }
    }

    private static class UnescapedSequenceFix extends CsvLocalQuickFix {
        @NotNull
        public String getFamilyName() {
            return "Surround with quotes";
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            try {
                PsiElement element = descriptor.getPsiElement();
                Document document = PsiDocumentManager.getInstance(project).getDocument(element.getContainingFile());
                List<Integer> quotePositions = new ArrayList<>();

                int quotePosition = CsvIntentionHelper.getOpeningQuotePosition(element);
                if (quotePosition != -1) {
                    quotePositions.add(quotePosition);
                }
                PsiElement endSeparatorElement = CsvIntentionHelper.findQuotePositionsUntilSeparator(element, quotePositions);
                if (endSeparatorElement == null) {
                    quotePositions.add(document.getTextLength());
                } else {
                    quotePositions.add(endSeparatorElement.getTextOffset());
                }
                String text = CsvIntentionHelper.addQuotes(document.getText(), quotePositions);
                document.setText(text);
            } catch (IncorrectOperationException e) {
                LOG.error(e);
            }
        }
    }

    private static class SeparatorMissingFix extends CsvLocalQuickFix {
        @NotNull
        public String getFamilyName() {
            return "Add separator";
        }

        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            try {
                PsiElement element = descriptor.getPsiElement();
                Document document = PsiDocumentManager.getInstance(project).getDocument(element.getContainingFile());
                String separator = CsvCodeStyleSettings.getCurrentSeparator(project, element.getContainingFile());
                String text = document.getText();
                document.setText(text.substring(0, element.getTextOffset()) + separator + text.substring(element.getTextOffset()));
            } catch (IncorrectOperationException e) {
                LOG.error(e);
            }
        }
    }

    private static class ClosingQuoteMissingFix extends CsvLocalQuickFix {
        @NotNull
        public String getFamilyName() {
            return "Add closing quote";
        }

        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            try {
                PsiElement element = descriptor.getPsiElement();
                Document document = PsiDocumentManager.getInstance(project).getDocument(element.getContainingFile());
                document.setText(document.getText() + "\"");
            } catch (IncorrectOperationException e) {
                LOG.error(e);
            }
        }
    }

}