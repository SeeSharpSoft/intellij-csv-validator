package net.seesharpsoft.intellij.plugins.csv.intention;

import com.intellij.codeInspection.InspectionToolProvider;

public class CsvSyntaxInspectionProvider implements InspectionToolProvider {
    public Class[] getInspectionClasses() {
        return new Class[]{CsvSyntaxInspection.class};
    }
}