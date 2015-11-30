package me.sgeb.gradle.nativeartifacts.internal

import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.NAR_GROUP
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.classifierForBinary
import me.sgeb.gradle.nativeartifacts.NativeComponent

import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.DomainObjectContext
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.internal.service.ServiceRegistry
import org.gradle.model.ModelMap
import org.gradle.model.Mutate
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.NativeComponentSpec
import org.gradle.nativeplatform.NativeExecutableBinarySpec
import org.gradle.nativeplatform.NativeExecutableSpec
import org.gradle.nativeplatform.NativeLibraryBinarySpec
import org.gradle.nativeplatform.NativeLibrarySpec
import org.gradle.nativeplatform.internal.AbstractNativeLibraryBinarySpec

class BuildNarTaskCreator extends RuleSource {

    public static final String NAR_LIFECYCLE_TASK_NAME = 'buildNar'
    public static final String NAR_BUILD_TASK_PREFIX = 'buildNar'

    @SuppressWarnings("GroovyUnusedDeclaration")
    @Mutate
    public void createBuildNarTasks(TaskContainer tasks, ModelMap<NativeComponentSpec> components, ServiceRegistry serviceRegistry) {

        final ProjectInternal project = serviceRegistry.get(DomainObjectContext)
        final Task narLifecycleTask = tasks[NAR_LIFECYCLE_TASK_NAME]

        for (final NativeComponentSpec component : components.values()) {
            if (targetedComponentType(component)) {
                NativeComponent nativeComponent = project.components.findByName(component.name)

                component.binaries.each { NativeBinarySpec binary ->
                    if (binary.buildable) {
                        AbstractArchiveTask task = createBuildNarTask(tasks, project, binary)
                        binary.tasks.add(task)

                        narLifecycleTask.dependsOn(task)
                        nativeComponent.from(task)
                    }
                }
            }
        }
    }

    private boolean targetedComponentType(NativeComponentSpec nativeComponent) {
        return nativeComponent instanceof NativeExecutableSpec || nativeComponent instanceof NativeLibrarySpec
    }

    private AbstractArchiveTask createBuildNarTask(TaskContainer tasks, ProjectInternal project, NativeBinarySpec binary) {
        Configuration configuration = project.configurations[binary.narConfName]
        File destDir = project.file("$project.buildDir/nar-bundles")

        def narTaskName = getBuildNarTaskName(binary)
        if (tasks.findByName(narTaskName) != null) {
            return tasks.maybeCreate(narTaskName, Nar)
        }

        Nar narTask = tasks.create(narTaskName, Nar) {
            group = NAR_GROUP
            description = "Builds native artifact for $binary.namingScheme.description."
            dependsOn binary.namingScheme.lifecycleTaskName
            conf configuration

            destinationDir destDir

            from (binary.primaryOutput) {
                into intoZipDirectory(binary)
            }

            if (binary instanceof AbstractNativeLibraryBinarySpec) {
                from((binary as AbstractNativeLibraryBinarySpec).headerDirs) {
                    into 'include'
                }
            }

            baseName = binary.component.name
            classifier = classifierForBinary(binary)
        }

        return narTask
    }

    private static String getBuildNarTaskName(NativeBinarySpec binary) {
        return binary.namingScheme.getTaskName(NAR_BUILD_TASK_PREFIX)
    }

    private static String intoZipDirectory(NativeBinarySpec binary) {
        if (binary instanceof NativeExecutableBinarySpec) {
            return 'bin'
        }
        if (binary instanceof NativeLibraryBinarySpec) {
            return 'lib'
        }
        return ''
    }

}
