package net.seesharpsoft.intellij.plugins.csv.editor.table;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.ExpectedHighlightingData;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

/**
 * This class provides a common API for the incompatible change of the 'checkResult' method, that happened in 202.*
 */
public class ExpectedHighlightingDataWrapper extends ExpectedHighlightingData {

    public ExpectedHighlightingDataWrapper(@NotNull Document document, boolean checkWarnings, boolean checkWeakWarnings, boolean checkInfos) {
        super(document, checkWarnings, checkWeakWarnings, checkInfos);
    }

    private boolean findMethodAndInvoke(String name, Object... parameters) {
        Class[] classes = Arrays.stream(parameters).map(obj -> obj.getClass()).toArray(n -> new Class[n]);
        Method methodToCall;
        try {
            methodToCall = ExpectedHighlightingData.class.getMethod("checkResult", classes);
            methodToCall.invoke(this, parameters);
        } catch (NoSuchMethodException e) {
            return false;
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void checkResultWrapper(PsiFile psiFile, Collection<? extends HighlightInfo> infos, String text) {
        if(!findMethodAndInvoke("checkResult", PsiFile.class, Collection.class, String.class)) {
            findMethodAndInvoke("checkResult", Collection.class, String.class);
        }
    }
}
