package me.sgeb.gradle.nativeartifacts.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gradle.nativeplatform.NativeBinarySpec;
import org.gradle.nativeplatform.test.NativeTestSuiteBinarySpec;

public class NativeDependencyContainer {
    public NativeDependencyContainer() {
        testComponentNames = new HashSet<String>();

        compileLibraries = new LinkedList<Map<String, String>>();
        testCompileLibraries = new LinkedList<Map<String, String>>();

        downloadedCompileLibraries = new LinkedList<Map<String, String>>();
        downloadedTestCompileLibraries = new LinkedList<Map<String, String>>();
    }

    public void testComponent(String name) {
        testComponentNames.add(name);
    }

    public boolean isTestBinary(NativeBinarySpec binary) {
        return binary instanceof NativeTestSuiteBinarySpec || isDeclaredTestBinary(binary);
    }

    public boolean isDeclaredTestBinary(NativeBinarySpec binary) {
        return testComponentNames.contains(binary.getComponent().getName());
    }

    public void compile(Map<String, String> library) {
        addLibrary(library, compileLibraries, downloadedCompileLibraries);
    }

    public void testCompile(Map<String, String> library) {
        addLibrary(library, testCompileLibraries, downloadedTestCompileLibraries);
    }

    private void addLibrary(Map<String, String> library, List<Map<String, String>> libraries, List<Map<String, String>> downloadedLibraries) {
        if (library.containsKey("group")) {
            downloadedLibraries.add(library);
        } else {
            libraries.add(library);
        }
    }

    public List<Map<String, String>> getCompileLibraries() {
        return Collections.unmodifiableList(compileLibraries);
    }

    public List<Map<String, String>> getTestCompileLibraries() {
        return Collections.unmodifiableList(testCompileLibraries);
    }

    public List<Map<String, String>> getDownloadedCompileLibraries() {
        return Collections.unmodifiableList(downloadedCompileLibraries);
    }

    public List<Map<String, String>> getDownloadedTestCompileLibraries() {
        return Collections.unmodifiableList(downloadedTestCompileLibraries);
    }

    private final Set<String> testComponentNames;
    private final List<Map<String, String>> compileLibraries;
    private final List<Map<String, String>> testCompileLibraries;

    private final List<Map<String, String>> downloadedCompileLibraries;
    private final List<Map<String, String>> downloadedTestCompileLibraries;
}
