package me.sgeb.gradle.nativeartifacts.internal;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class NativeDependencyContainer {
    public NativeDependencyContainer() {
        compileLibraries = new LinkedList<Map<String, String>>();
        runtimeLibraries = new LinkedList<Map<String, String>>();
        testCompileLibraries = new LinkedList<Map<String, String>>();
        testRuntimeLibraries = new LinkedList<Map<String, String>>();

        downloadedCompileLibraries = new LinkedList<Map<String, String>>();
        downloadedRuntimeLibraries = new LinkedList<Map<String, String>>();
        downloadedTestCompileLibraries = new LinkedList<Map<String, String>>();
        downloadedTestRuntimeLibraries = new LinkedList<Map<String, String>>();
    }

    public void compile(Map<String, String> library) {
        addLibrary(library, compileLibraries, downloadedCompileLibraries);
    }

    public void runtime(Map<String, String> library) {
        addLibrary(library, runtimeLibraries, downloadedRuntimeLibraries);
    }

    public void testCompile(Map<String, String> library) {
        addLibrary(library, testCompileLibraries, downloadedTestCompileLibraries);
    }

    public void testRuntime(Map<String, String> library) {
        addLibrary(library, testRuntimeLibraries, downloadedTestRuntimeLibraries);
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

    public List<Map<String, String>> getRuntimeLibraries() {
        return Collections.unmodifiableList(runtimeLibraries);
    }

    public List<Map<String, String>> getTestCompileLibraries() {
        return Collections.unmodifiableList(testCompileLibraries);
    }

    public List<Map<String, String>> getTestRuntimeLibraries() {
        return Collections.unmodifiableList(testRuntimeLibraries);
    }

    public List<Map<String, String>> getDownloadedCompileLibraries() {
        return Collections.unmodifiableList(downloadedCompileLibraries);
    }

    public List<Map<String, String>> getDownloadedRuntimeLibraries() {
        return Collections.unmodifiableList(downloadedRuntimeLibraries);
    }

    public List<Map<String, String>> getDownloadedTestCompileLibraries() {
        return Collections.unmodifiableList(downloadedTestCompileLibraries);
    }

    public List<Map<String, String>> getDownloadedTestRuntimeLibraries() {
        return Collections.unmodifiableList(downloadedTestRuntimeLibraries);
    }

    private final List<Map<String, String>> compileLibraries;
    private final List<Map<String, String>> runtimeLibraries;
    private final List<Map<String, String>> testCompileLibraries;
    private final List<Map<String, String>> testRuntimeLibraries;

    private final List<Map<String, String>> downloadedCompileLibraries;
    private final List<Map<String, String>> downloadedRuntimeLibraries;
    private final List<Map<String, String>> downloadedTestCompileLibraries;
    private final List<Map<String, String>> downloadedTestRuntimeLibraries;
}
