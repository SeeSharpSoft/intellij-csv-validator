package net.seesharpsoft.intellij.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.SyntaxTraverser;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

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
            if (sibling instanceof PsiErrorElement) return null;
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
             sibling = backwards ? sibling.getPrevSibling() : sibling.getNextSibling()) {
            if (sibling instanceof PsiErrorElement) return null;
            if (getElementType(sibling) == type) {
                return sibling;
            }
        }
        return null;
    }

    public static PsiElement getLastChildOfType(@NotNull final PsiElement parent, @NotNull IElementType type) {
        if (parent.getFirstChild() == null) return null;
        return getLastSiblingOfType(parent.getFirstChild(), type, false);
    }

    public static PsiElement getFirstSiblingOfType(@NotNull final PsiElement element, @NotNull IElementType type) {
        return getLastSiblingOfType(element, type, true);
    }

    public static PsiElement getLastSiblingOfType(@NotNull final PsiElement element, @NotNull IElementType type) {
        return getLastSiblingOfType(element, type, false);
    }

    public static PsiElement getLastSiblingOfType(@NotNull final PsiElement element, @NotNull IElementType type, boolean backwards) {
        PsiElement nextSibling = element;
        PsiElement prevSibling;
        do {
            prevSibling = nextSibling;
            nextSibling = getSiblingOfType(prevSibling, type, backwards);
        } while (nextSibling != null);
        return prevSibling == element ? null : prevSibling;
    }

    public static PsiElement findFirst(@Nullable final PsiElement root, @NotNull IElementType type) {
        return SyntaxTraverser.psiTraverser(root).filterTypes(elementType -> elementType == type).filter(PsiElement.class).first();
    }

    public static void traverseChildrenOfAnyType(@NotNull final PsiElement parent, Consumer<PsiElement> consume, IElementType... types) {
        Set<IElementType> allTypes = new HashSet<>(Arrays.asList(types));
        for (PsiElement child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (allTypes.contains(getElementType(child))) consume.accept(child);
        }
    }

    public static Collection<PsiElement> findChildrenOfAnyElement(@NotNull final PsiElement parent, IElementType... types) {
        List<PsiElement> foundElements = new ArrayList<>();
        traverseChildrenOfAnyType(parent, element -> foundElements.add(element), types);
        return foundElements;
    }
}
