package me.sgeb.gradle.nativeartifacts.internal

import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.collections.SimpleFileCollection
import org.gradle.nativeplatform.NativeDependencySet

class DownloadedNativeDependencySet implements NativeDependencySet {
    final FileCollection includeRoots
    final FileCollection linkFiles
    final FileCollection runtimeFiles

    DownloadedNativeDependencySet(File includePath, File linkLibraryPath, File libraryPath) {
        includeRoots = new SimpleFileCollection(includePath)
        linkFiles = new SimpleFileCollection(linkLibraryPath)
        runtimeFiles = new SimpleFileCollection(libraryPath)
    }

    DownloadedNativeDependencySet(File includePath) {
        includeRoots = new SimpleFileCollection(includePath)
        linkFiles = new SimpleFileCollection()
        runtimeFiles = new SimpleFileCollection()
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
