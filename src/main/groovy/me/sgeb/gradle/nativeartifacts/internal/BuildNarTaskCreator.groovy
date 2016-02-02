package me.sgeb.gradle.nativeartifacts.internal

import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.NAR_GROUP
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.classifierForBinary
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.getCompileConfigurationName
import me.sgeb.gradle.nativeartifacts.NativeComponent

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.DomainObjectContext
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.model.Defaults
import org.gradle.model.RuleSource
import org.gradle.model.ModelMap
import org.gradle.internal.service.ServiceRegistry
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.NativeComponentSpec
import org.gradle.nativeplatform.NativeExecutableBinarySpec
import org.gradle.nativeplatform.NativeExecutableSpec
import org.gradle.nativeplatform.NativeLibraryBinarySpec
import org.gradle.nativeplatform.NativeLibrarySpec
import org.gradle.nativeplatform.SharedLibraryBinarySpec
import org.gradle.nativeplatform.StaticLibraryBinarySpec
import org.gradle.nativeplatform.internal.AbstractNativeLibraryBinarySpec

class BuildNarTaskCreator extends RuleSource {

    public static final String NAR_LIFECYCLE_TASK_NAME = 'buildNar'
    public static final String NAR_BUILD_TASK_PREFIX = 'buildNar'

    @SuppressWarnings("GroovyUnusedDeclaration")
    @Defaults
    public void createBuildNarTasks(TaskContainer tasks, ModelMap<NativeBinarySpec> binaries, ServiceRegistry serviceRegistry) {

        Project project = serviceRegistry.get(DomainObjectContext)
        Task narLifecycleTask = tasks[NAR_LIFECYCLE_TASK_NAME]

        binaries.each { NativeBinarySpec binary ->
            if (binary.buildable) {
                NativeComponentSpec component = binary.getComponent()
                if (targetedComponentType(component)) {
                    NativeComponent nativeComponent = project.components.findByName(component.name)

                    if (nativeComponent == null) {
                        nativeComponent = new NativeComponent(component.name)
                        nativeComponent.from(component)
                        project.components.add(nativeComponent)
                    }

                    AbstractArchiveTask task = createBuildNarTask(tasks, project, binary)
                    binary.tasks.add(task)

                    narLifecycleTask.dependsOn(task)
                    nativeComponent.from(task)
                }
            }
        }
    }

    private boolean targetedComponentType(NativeComponentSpec nativeComponent) {
        return nativeComponent instanceof NativeExecutableSpec || nativeComponent instanceof NativeLibrarySpec
    }

    private AbstractArchiveTask createBuildNarTask(TaskContainer tasks, Project project, NativeBinarySpec binary) {
        Configuration configuration = project.configurations[getCompileConfigurationName(binary)]
        File destDir = project.file("$project.buildDir/nar-bundles")

        def narTaskName = getBuildNarTaskName(binary)
        Nar narTask = tasks.findByName(narTaskName)
        if (narTask == null) {
            narTask = tasks.create(narTaskName, Nar, new Action<Nar>() {
                @Override
                public void execute(Nar nar) {
                    nar.group = NAR_GROUP
                    nar.description = "Builds native artifact for $binary.namingScheme.description."
                    setTaskBuildDependency(nar, binary)
                    nar.conf configuration
                    nar.destinationDir destDir

                    nar.from (binary.primaryOutput) {
                        into intoZipDirectory(binary)
                    }

                    if (binary instanceof NativeLibraryBinarySpec) {
                        nar.from(binary.headerDirs) {
                            into 'include'
                            exclude '**/*.cpp', '**/*.c'
                        }
                    }

                    nar.baseName = binary.component.name
                    nar.classifier = classifierForBinary(binary)
                }
            })
        }

        return narTask
    }

    private String getBuildNarTaskName(NativeBinarySpec binary) {
        return binary.namingScheme.getTaskName(NAR_BUILD_TASK_PREFIX)
    }

    private void setTaskBuildDependency(Nar nar, NativeBinarySpec binary) {
        if (binary instanceof NativeExecutableBinarySpec) {
            nar.dependsOn binary.tasks.link
        }
        if (binary instanceof SharedLibraryBinarySpec) {
            nar.dependsOn binary.tasks.link
        }
        if (binary instanceof StaticLibraryBinarySpec) {
            nar.dependsOn binary.tasks.createStaticLib
        }
    }

    private String intoZipDirectory(NativeBinarySpec binary) {
        if (binary instanceof NativeExecutableBinarySpec) {
            return 'bin'
        }
        if (binary instanceof NativeLibraryBinarySpec) {
            return 'lib'
        }
        return ''
    }

}
