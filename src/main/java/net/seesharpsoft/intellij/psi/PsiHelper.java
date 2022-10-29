package net.seesharpsoft.intellij.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.SyntaxTraverser;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import net.seesharpsoft.intellij.plugins.csv.psi.CsvField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PsiHelper {
    private PsiHelper() {
        // static helper
    }

    public static IElementType getElementType(PsiElement element) {
        return element == null || element.getNode() == null ? null : element.getNode().getElementType();
    }

    @Nullable
    public static <T extends PsiElement> T getNthChildOfType(@NotNull final PsiElement parent, int n, @NotNull Class<T> aClass) {
        PsiElement firstChild = parent.getFirstChild();
        return firstChild == null ? null : getNextNthSiblingOfType(firstChild, n, aClass);
    }

    @Nullable
    public static <T extends PsiElement> T getPrevNthSiblingOfType(@NotNull final PsiElement element, int n, @NotNull Class<T> aClass) {
        return getNthSiblingOfType(element, n, aClass, false);
    }

    @Nullable
    public static <T extends PsiElement> T getNextNthSiblingOfType(@NotNull final PsiElement element, int n, @NotNull Class<T> aClass) {
        return getNthSiblingOfType(element, n, aClass, false);
    }

    @Nullable
    public static <T extends PsiElement> T getNthSiblingOfType(@NotNull final PsiElement element, int n, @NotNull Class<T> aClass, boolean backwards) {
        if (n < 0) return null;
        int count = 0;
        for (PsiElement sibling = element; sibling != null; sibling = backwards ? sibling.getPrevSibling() : sibling.getNextSibling()) {
            if (aClass.isInstance(sibling)) {
                if (count == n) return aClass.cast(sibling);
                ++count;
            }
        }
        return null;
    }

    public static <T extends PsiElement> int getChildIndexOfType(@NotNull final PsiElement parent, @NotNull T targetChild, @NotNull Class<T> aClass) {
        int index = 0;
        for (PsiElement child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (targetChild == child) return index;
            if (aClass.isInstance(child)) {
                ++index;
            }
        }
        return -1;
    }

    public static PsiElement getNextSiblingOfType(@NotNull final PsiElement element, @NotNull IElementType type) {
        return getSiblingOfType(element, type, false);
    }

    public static PsiElement getPrevSiblingOfType(@NotNull final PsiElement element, @NotNull IElementType type) {
        return getSiblingOfType(element, type, true);
    }

    public static PsiElement getSiblingOfType(@NotNull final PsiElement element, @NotNull IElementType type, boolean backwards) {
        for (PsiElement sibling = backwards ? element.getPrevSibling() : element.getNextSibling();
             sibling != null;
             sibling = backwards ? sibling.getPrevSibling() : sibling.getNextSibling())
        {
            if (getElementType(sibling) == type) {
                return sibling;
            }
        }
        return null;
    }

    public static PsiElement findFirst(@NotNull final PsiElement root, @NotNull IElementType type) {
        return SyntaxTraverser.psiTraverser(root).filterTypes(elementType -> elementType == type).filter(PsiElement.class).first();
    }
}
