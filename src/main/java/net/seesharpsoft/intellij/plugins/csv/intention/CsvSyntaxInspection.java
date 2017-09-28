package net.seesharpsoft.intellij.plugins.csv.intention;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.IncorrectOperationException;
import net.seesharpsoft.intellij.plugins.csv.CsvLanguage;
import net.seesharpsoft.intellij.plugins.csv.formatter.CsvCodeStyleSettings;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvTypes;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author max
 */
public class CsvSyntaxInspection extends LocalInspectionTool {
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
                if (element == null || element.getLanguage() != CsvLanguage.INSTANCE) {
                    return;
                }
                
                IElementType elementType = CsvIntentionHelper.getElementType(element);
                PsiElement firstChild = element.getFirstChild();
                PsiElement nextSibling = element.getNextSibling();
                if (elementType == TokenType.ERROR_ELEMENT && CsvIntentionHelper.getElementType(firstChild) == TokenType.BAD_CHARACTER) {
                    if (firstChild.getText().equals("\"")) {
                        holder.registerProblem(element, UNESCAPED_SEQUENCE, fixUnescapedSequence);
                    } else {
                        holder.registerProblem(element, SEPARATOR_MISSING, fixSeparatorMissing);
                        holder.registerProblem(element, UNESCAPED_SEQUENCE, fixUnescapedSequence);
                    }
                } else if ((elementType == CsvTypes.TEXT || elementType == CsvTypes.ESCAPED_TEXT)
                        && CsvIntentionHelper.getElementType(nextSibling) == TokenType.ERROR_ELEMENT
                        && nextSibling.getFirstChild() == null) {
                    holder.registerProblem(element, CLOSING_QUOTE_MISSING, fixClosingQuoteMissing);
                }
            }
        };
    }
    
    private static abstract class CsvLocalQuickFix implements LocalQuickFix {
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
                String text = document.getText();
                document.setText(text.substring(0, element.getTextOffset()) + CsvCodeStyleSettings.getCurrentSeparator(project) + text.substring(element.getTextOffset()));
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