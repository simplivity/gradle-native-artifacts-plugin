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
import org.gradle.nativeplatform.NativeLibraryBinarySpec
import org.gradle.nativeplatform.SharedLibraryBinarySpec
import org.gradle.nativeplatform.StaticLibraryBinarySpec
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
            if (ConfigurationCreator.targetedComponentType(component)) {
                NativeComponent nativeComponent = project.components.findByName(component.name)

                if (nativeComponent == null) {
                    nativeComponent = new NativeComponent(component.name)
                    nativeComponent.from(component)
                    project.components.add(nativeComponent)
                }

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

    private AbstractArchiveTask createBuildNarTask(TaskContainer tasks, ProjectInternal project, NativeBinarySpec binary) {
        Configuration configuration = project.configurations[binary.narConfName]
        File destDir = project.file("$project.buildDir/nar-bundles")
        def narTaskName = getBuildNarTaskName(binary)
        Nar narTask = tasks.maybeCreate(narTaskName, Nar)
        narTask.configure {
            description = "Builds native artifact for $binary.namingScheme.description."
            group = NAR_GROUP
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
