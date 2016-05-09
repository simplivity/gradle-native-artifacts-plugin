package me.sgeb.gradle

import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.*

import javax.inject.Inject

import me.sgeb.gradle.nativeartifacts.internal.BuildNarTaskCreator
import me.sgeb.gradle.nativeartifacts.internal.ConfigurationCreator
import me.sgeb.gradle.nativeartifacts.internal.ExtractNarDepsTaskCreator
import me.sgeb.gradle.nativeartifacts.internal.NativeArtifactPublisher
import me.sgeb.gradle.nativeartifacts.internal.NativeDependenciesRules
import me.sgeb.gradle.nativeartifacts.internal.SourceZipCreator
import me.sgeb.gradle.nativeartifacts.internal.SvtVersionTagCreator

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.model.internal.registry.ModelRegistry
import org.gradle.model.internal.type.ModelType
import org.gradle.nativeplatform.NativeComponentSpec
import org.gradle.nativeplatform.plugins.NativeComponentPlugin

class NativeArtifactsPlugin implements Plugin<Project> {
    private final ModelRegistry modelRegistry

    @Inject
    NativeArtifactsPlugin(ModelRegistry modelRegistry) {
        this.modelRegistry = modelRegistry
    }

    @Override
    void apply(Project project) {
        project.pluginManager.apply(NativeComponentPlugin)

        project.pluginManager.apply(ExtractNarDepsTaskCreator)
        project.pluginManager.apply(BuildNarTaskCreator)
        project.pluginManager.apply(SvtVersionTagCreator)
        project.pluginManager.apply(SourceZipCreator)
        project.pluginManager.apply(NativeDependenciesRules)
        project.pluginManager.apply(NativeArtifactPublisher)

        modelRegistry.getRoot().applyToAllLinksTransitive(ModelType.of(NativeComponentSpec), ConfigurationCreator)

        project.configurations.maybeCreate(NAR_COMPILE_CONFIGURATION_PREFIX)
        project.configurations.maybeCreate(NAR_TEST_COMPILE_CONFIGURATION_PREFIX)

        project.tasks.create(BuildNarTaskCreator.NAR_LIFECYCLE_TASK_NAME).configure {
            description = 'Builds all native artifact archives on all buildable platforms.'
            group = NAR_GROUP
        }
    }
}
