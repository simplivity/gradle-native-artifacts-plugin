package me.sgeb.gradle

import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.NAR_GROUP
import me.sgeb.gradle.nativeartifacts.internal.BuildNarTaskCreator
import me.sgeb.gradle.nativeartifacts.internal.ConfigurationCreator
import me.sgeb.gradle.nativeartifacts.internal.DownloadedNativeDependencySet
import me.sgeb.gradle.nativeartifacts.internal.ExtractNarDepsTaskCreator

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

        // Add a dynamic method to the project to define a NativeDependencySet
        // for a downloaded library capturing the include path and library path.
        project.ext.downloadedLibraryDependency = { NativeBinarySpec binary, String libName ->
            File includePath = new File(binary.narDepsDir, "include")
            File libraryPath = new File(binary.narDepsDir, "lib/lib${libName}.so")
            return new DownloadedNativeDependencySet(includePath, libraryPath)
        }
    }
}
