package net.seesharpsoft.intellij.plugins.csv;

import junit.framework.TestCase;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class CsvTablePackageDependenciesTest extends TestCase {
    private static final String PREFIX = "net.seesharpsoft.intellij.plugins.csv.";
    private static final List<String> PACKAGES = List.of(
            "editor.table.swing", "editor.table", "editor", "table.ui", "table", "diff");
    private static final Map<String, Set<String>> DEPENDENCIES = Map.of(
            "editor.table.swing", Set.of("editor.table", "table.ui"),
            "editor.table", Set.of("editor", "table"),
            "editor", Set.of(),
            "table.ui", Set.of("table"),
            "table", Set.of(),
            "diff", Set.of("table.ui"));
    private static final Pattern PACKAGE = Pattern.compile("(?m)^package\\s+([\\w.]+);");
    private static final Pattern REFERENCE = Pattern.compile(
            "\\bnet\\.seesharpsoft\\.intellij\\.plugins\\.csv\\.([\\w.]+)");
    private static final Pattern NON_CODE = Pattern.compile(
            "//[^\\n]*|/\\*[\\s\\S]*?\\*/|\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'");

    public void testProductionPackagesFollowTheTableDependencyGraph() throws Exception {
        Path sources = Path.of("src", "main", "java", "net", "seesharpsoft", "intellij", "plugins", "csv");
        assertTrue("Production sources must be available", Files.isDirectory(sources));
        List<String> violations = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        try (var files = Files.walk(sources)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String code = NON_CODE.matcher(Files.readString(file)).replaceAll("");
                var declaration = PACKAGE.matcher(code);
                assertTrue("Missing package declaration in " + file, declaration.find());
                String source = scopedPackage(declaration.group(1));
                if (source == null) continue;
                visited.add(source);
                Set<String> allowed = allowedDependencies(source);
                var references = REFERENCE.matcher(code);
                while (references.find()) {
                    String target = scopedPackage(PREFIX + references.group(1));
                    if (target != null && !source.equals(target) && !allowed.contains(target)) {
                        violations.add(sources.relativize(file) + ": " + source + " -> " + target
                                + " (" + references.group() + ")");
                    }
                }
            }
        }
        assertTrue("No production packages were checked", !visited.isEmpty());
        assertTrue("Forbidden table package dependencies:\n" + String.join("\n", violations), violations.isEmpty());
    }

    private static String scopedPackage(String name) {
        for (String candidate : PACKAGES) {
            String qualified = PREFIX + candidate;
            if (name.equals(qualified) || name.startsWith(qualified + ".")) return candidate;
        }
        return null;
    }

    private static Set<String> allowedDependencies(String source) {
        Set<String> allowed = new HashSet<>();
        ArrayDeque<String> remaining = new ArrayDeque<>(DEPENDENCIES.get(source));
        while (!remaining.isEmpty()) {
            String target = remaining.removeFirst();
            if (allowed.add(target)) remaining.addAll(DEPENDENCIES.get(target));
        }
        return allowed;
    }
}
