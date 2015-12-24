package me.sgeb.gradle.nativeartifacts.internal

import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.getCompileConfigurationName
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.getTestConfigurationName
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.getNarCompileDepsDir
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.getNarTestDepsDir

import org.gradle.api.Project
import org.gradle.api.Named
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.test.NativeTestSuiteBinarySpec

class FunctionHelpers {
    public static void addFunctions(Project project) {
        // Add a dynamic method to the project to set a dependency
        // for a downloaded library.
        // Currently this takes the following arguments:
        // binary:  the NativeBinarySpec that we are adding a dependency for
        // group:   the group specification for the dependency artifact.
        // name:    the simple name of the library as a string.
        // version: the Maven version of the downloaded artifact.
        // linkage: the string 'static' or 'shared' only. This plugin
        //          does not generate 'api' libraries yet.
        project.ext.addDownloadedLibraryDependency = {
            NativeBinarySpec binary, String group, String name, String version, String linkage ->
            def confName = getCompileConfigurationName(binary)
            def narDepsDir = getNarCompileDepsDir(project.buildDir, binary)

            addLibDependency(project, binary, group, name, version, linkage, confName, narDepsDir, linkage)
        }

        project.ext.addDownloadedLibraryTestDependency = {
            NativeBinarySpec binary, String group, String name, String version, String linkage ->
            def confName = getTestConfigurationName(binary)
            def narDepsDir = getNarTestDepsDir(project.buildDir, binary)

            addLibDependency(project, binary, group, name, version, linkage, confName, narDepsDir, linkage)
        }

        project.ext.addDownloadedApiLibraryDependency = {
            NativeBinarySpec binary, String group, String name, String version, String linkage ->
            def confName = getCompileConfigurationName(binary)
            def narDepsDir = getNarCompileDepsDir(project.buildDir, binary)

            addLibDependency(project, binary, group, name, version, linkage, confName, narDepsDir, 'api')
        }

        project.ext.addDownloadedApiLibraryTestDependency = {
            NativeBinarySpec binary, String group, String name, String version, String linkage ->
            def confName = getTestConfigurationName(binary)
            def narDepsDir = getNarTestDepsDir(project.buildDir, binary)

            addLibDependency(project, binary, group, name, version, linkage, confName, narDepsDir, 'api')
        }
    }

    private static void addLibDependency(Project project, NativeBinarySpec binary,
                            String group, String name, String version, String linkage,
                         String confName, File narDepsDir, String libType) {
        assert linkage == 'static' || linkage == 'shared'

        def classifier = "${binary.targetPlatform.name}-${linkage}-${binary.buildType.name}"
        def depMap = [ group: group, name: name, version: version, ext: 'nar', classifier: classifier ]

        project.dependencies {
            add confName, depMap
        }

        setDownloadedLibrary(binary, narDepsDir, name, libType)
    }

    // Define a NativeDependencySet for a downloaded library capturing the include
    // path and library path. Currently this takes the following arguments:
    // binary:  the NativeBinarySpec that we are adding a dependency for
    // name:    the simple name of the library as a string
    // linkage: the string 'static', 'shared' or 'api' to handle the type of library
    private static void setDownloadedLibrary(NativeBinarySpec binary, File narDepsDir, String name, String linkage) {
        File includePath = new File(narDepsDir, "include")
        File libraryFile, linkLibraryFile

        if (linkage == 'shared') {
            libraryFile = sharedLibrary(binary, narDepsDir, name)
            linkLibraryFile = linkLibrary(binary, narDepsDir, name)
            binary.lib new DownloadedNativeDependencySet(includePath, linkLibraryFile, libraryFile)
        } else if (linkage == 'static') {
            libraryFile = staticLibrary(binary, narDepsDir, name)
            linkLibraryFile = libraryFile
            binary.lib new DownloadedNativeDependencySet(includePath, linkLibraryFile, libraryFile)
        } else if (linkage == 'api') {
            binary.lib new DownloadedNativeDependencySet(includePath)
        } else {
            throw new RuntimeException("Unsupported linkage type $linkage")
        }
    }

    private static File sharedLibrary(NativeBinarySpec binary, File narDepsDir, String libName) {
        return new File(narDepsDir,
            (binary.targetPlatform.operatingSystem.windows) ?
                 "lib/${libName}.dll" : "lib/lib${libName}.so")
    }

    private static File staticLibrary(NativeBinarySpec binary, File narDepsDir, String libName) {
        return new File(narDepsDir,
            (binary.targetPlatform.operatingSystem.windows) ?
                 "lib/${libName}.lib" : "lib/lib${libName}.a")
    }

    private static File linkLibrary(NativeBinarySpec binary, File narDepsDir, String libName) {
        return new File(narDepsDir,
            (binary.targetPlatform.operatingSystem.windows) ?
                 "lib/${libName}.lib" : "lib/lib${libName}.so")
    }
}