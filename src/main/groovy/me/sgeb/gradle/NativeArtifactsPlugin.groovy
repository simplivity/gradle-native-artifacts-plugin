package me.sgeb.gradle

import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.NAR_COMPILE_CONFIGURATION_PREFIX
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.NAR_GROUP
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.getConfigurationNameVar
import me.sgeb.gradle.nativeartifacts.internal.BuildNarTaskCreator
import me.sgeb.gradle.nativeartifacts.internal.ConfigurationCreator
import me.sgeb.gradle.nativeartifacts.internal.DownloadedNativeDependencySet
import me.sgeb.gradle.nativeartifacts.internal.ExtractNarDepsTaskCreator

import org.gradle.api.Named
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskContainer
import org.gradle.internal.reflect.Instantiator
import org.gradle.model.internal.registry.ModelRegistry
import org.gradle.model.internal.type.ModelType
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.NativeComponentSpec
import org.gradle.nativeplatform.plugins.NativeComponentPlugin

import javax.inject.Inject

class NativeArtifactsPlugin implements Plugin<Project> {

    static final String EXTENSION_NAME = "nativeArtifacts"

    private final Instantiator instantiator
    private final ModelRegistry modelRegistry

    @Inject
    NativeArtifactsPlugin(Instantiator instantiator, ModelRegistry modelRegistry) {
        this.instantiator = instantiator
        this.modelRegistry = modelRegistry
    }

    @Override
    void apply(Project project) {
        project.pluginManager.apply(NativeComponentPlugin)

        project.tasks.create(BuildNarTaskCreator.NAR_LIFECYCLE_TASK_NAME).configure {
            description = 'Builds all native artifacts on all buildable platforms.'
            group = NAR_GROUP
        }

        modelRegistry.getRoot().applyToAllLinksTransitive(ModelType.of(NativeComponentSpec), ConfigurationCreator)
        modelRegistry.getRoot().applyToAllLinksTransitive(ModelType.of(TaskContainer), BuildNarTaskCreator)
        modelRegistry.getRoot().applyToAllLinksTransitive(ModelType.of(TaskContainer), ExtractNarDepsTaskCreator)

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
            assert linkage == 'static' || linkage == 'shared'

            def classifier = "${binary.targetPlatform.name}-${linkage}-${binary.buildType.name}"
            def depMap = [ group: group, name: name, version: version, ext: 'nar', classifier: classifier ]
            project.dependencies {
                add binary.narConfName, depMap
            }

            setDownloadedLibrary(binary, name, linkage)
        }

        project.ext.addDownloadedApiLibraryDependency = {
            NativeBinarySpec binary, String group, String name, String version, String linkage ->
            assert linkage == 'static' || linkage == 'shared'

            def classifier = "${binary.targetPlatform.name}-${linkage}-${binary.buildType.name}"
            def depMap = [ group: group, name: name, version: version, ext: 'nar', classifier: classifier ]
            project.dependencies {
                add binary.narConfName, depMap
            }

            setDownloadedLibrary(binary, name, 'api')
        }

        // Add a dynamic method to the project to define a native configuration
        // name for a given combination of platform/buildType/flavor tuple.
        project.ext.narConfigurationName = { Named platform, Named buildType, Named flavor ->
            getConfigurationNameVar(NAR_COMPILE_CONFIGURATION_PREFIX, platform, buildType, flavor)
        }
    }

    // Define a NativeDependencySet for a downloaded library capturing the include
    // path and library path. Currently this takes the following arguments:
    // binary:  the NativeBinarySpec that we are adding a dependency for
    // name:    the simple name of the library as a string
    // linkage: the string 'static', 'shared' or 'api' to handle the type of library
    private void setDownloadedLibrary(NativeBinarySpec binary, String name, String linkage) {
        File includePath = new File(binary.narDepsDir, "include")
        File libraryFile, linkLibraryFile

        if (linkage == 'shared') {
            libraryFile = sharedLibrary(binary, name)
            linkLibraryFile = linkLibrary(binary, name)
            binary.lib new DownloadedNativeDependencySet(includePath, linkLibraryFile, libraryFile)
        } else if (linkage == 'static') {
            libraryFile = staticLibrary(binary, name)
            linkLibraryFile = libraryFile
            binary.lib new DownloadedNativeDependencySet(includePath, linkLibraryFile, libraryFile)
        } else if (linkage == 'api') {
            binary.lib new DownloadedNativeDependencySet(includePath)
        } else {
            throw RuntimeException("Unsupported linkage type $linkage")
        }
    }

    private File sharedLibrary(NativeBinarySpec binary, String libName) {
        return new File(binary.narDepsDir,
            (binary.targetPlatform.operatingSystem.windows) ?
                 "lib/${libName}.dll" : "lib/lib${libName}.so")
    }

    private File staticLibrary(NativeBinarySpec binary, String libName) {
        return new File(binary.narDepsDir,
            (binary.targetPlatform.operatingSystem.windows) ?
                 "lib/${libName}.lib" : "lib/lib${libName}.a")
    }

    private File linkLibrary(NativeBinarySpec binary, String libName) {
        return new File(binary.narDepsDir,
            (binary.targetPlatform.operatingSystem.windows) ?
                 "lib/${libName}.lib" : "lib/lib${libName}.so")
    }
}
