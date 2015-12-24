package me.sgeb.gradle

import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.NAR_COMPILE_CONFIGURATION_PREFIX
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.NAR_TEST_CONFIGURATION_PREFIX
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.NAR_GROUP

import me.sgeb.gradle.nativeartifacts.internal.FunctionHelpers
import me.sgeb.gradle.nativeartifacts.internal.BuildNarTaskCreator
import me.sgeb.gradle.nativeartifacts.internal.ConfigurationCreator
import me.sgeb.gradle.nativeartifacts.internal.ExtractNarDepsTaskCreator
import me.sgeb.gradle.nativeartifacts.NativeComponent

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskContainer
import org.gradle.nativeplatform.NativeComponentSpec
import org.gradle.nativeplatform.plugins.NativeComponentPlugin
import org.gradle.internal.reflect.Instantiator
import org.gradle.model.internal.registry.ModelRegistry
import org.gradle.model.internal.type.ModelType

import javax.inject.Inject

class NativeArtifactsPlugin implements Plugin<Project> {

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

        modelRegistry.getRoot().applyToAllLinksTransitive(ModelType.of(NativeComponentSpec), ConfigurationCreator)
        modelRegistry.getRoot().applyToAllLinksTransitive(ModelType.of(TaskContainer), BuildNarTaskCreator)
        modelRegistry.getRoot().applyToAllLinksTransitive(ModelType.of(TaskContainer), ExtractNarDepsTaskCreator)

        project.configurations.maybeCreate(NAR_COMPILE_CONFIGURATION_PREFIX)
        project.configurations.maybeCreate(NAR_TEST_CONFIGURATION_PREFIX)

        project.tasks.create(BuildNarTaskCreator.NAR_LIFECYCLE_TASK_NAME).configure {
            description = 'Builds all native artifact archives on all buildable platforms.'
            group = NAR_GROUP
        }

        FunctionHelpers.addFunctions(project)
    }
}
