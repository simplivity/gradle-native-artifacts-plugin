package me.sgeb.gradle.nativeartifacts.internal

import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.collections.FileCollectionAdapter
import org.gradle.api.internal.file.collections.ListBackedFileSet
import org.gradle.nativeplatform.NativeDependencySet

class DownloadedNativeDependencySet implements NativeDependencySet {
    final FileCollection includeRoots
    final FileCollection linkFiles
    final FileCollection runtimeFiles

    DownloadedNativeDependencySet(File includePath, File libraryPath) {
        //println "Constructed NativeDependencySet with $includePath and $libraryPath"
        includeRoots = new FileCollectionAdapter(new ListBackedFileSet(includePath))
        linkFiles = new FileCollectionAdapter(new ListBackedFileSet(libraryPath))
        runtimeFiles = new FileCollectionAdapter(new ListBackedFileSet(libraryPath))
    }

    FileCollection getIncludeRoots() {
        return includeRoots;
    }

    FileCollection getLinkFiles() {
        return linkFiles;
    }

    FileCollection getRuntimeFiles() {
        return runtimeFiles;
    }
}
