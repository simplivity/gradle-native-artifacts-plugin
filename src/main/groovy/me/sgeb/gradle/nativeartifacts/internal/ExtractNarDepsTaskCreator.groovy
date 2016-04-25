package me.sgeb.gradle.nativeartifacts.internal

import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.*

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.DomainObjectContext
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskContainer
import org.gradle.internal.service.ServiceRegistry
import org.gradle.model.Finalize
import org.gradle.model.ModelMap
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.test.NativeTestSuiteBinarySpec

class ExtractNarDepsTaskCreator extends RuleSource {

    @SuppressWarnings("GroovyUnusedDeclaration")
    @Finalize
    public void createExtractNarDependenciesTasks(TaskContainer tasks, ModelMap<NativeBinarySpec> binaries, ServiceRegistry serviceRegistry) {

        Project project = serviceRegistry.get(DomainObjectContext)

        for (NativeBinarySpec binary : binaries.values()) {
            Task extractCompileDepsTask = createExtractCompileDepsTask(binary, tasks, project)

            Task extractTestCompileDepsTask = null
            if (binary instanceof NativeTestSuiteBinarySpec) {
                extractTestCompileDepsTask = createExtractTestCompileDepsTask(binary, tasks, project)
            }

            getCompileTasks(binary).each { compileTask ->
                compileTask.dependsOn(extractCompileDepsTask)
                if (extractTestCompileDepsTask != null) {
                    compileTask.dependsOn(extractTestCompileDepsTask)
                }
            }
        }
    }

    private Task createExtractCompileDepsTask(NativeBinarySpec binary, TaskContainer tasks, Project project) {
        String compileName = getCompileConfigurationName(binary)
        String runtimeName = getRuntimeConfigurationName(binary)
        String taskName = getExtractNarCompileDepsTaskName(binary)
        File depsDir = getNarCompileDepsDir(project.buildDir, binary)

        Task task = createExtractNarDepsTask(compileName, runtimeName, taskName, depsDir, tasks, project)
        binary.tasks.add(task)
        return task
    }

    private Task createExtractTestCompileDepsTask(NativeBinarySpec binary, TaskContainer tasks, Project project) {
        String compileName = getTestCompileConfigurationName(binary)
        String runtimeName = getTestRuntimeConfigurationName(binary)
        String taskName = getExtractNarTestCompileDepsTaskName(binary)
        File depsDir = getNarTestCompileDepsDir(project.buildDir, binary)

        Task task = createExtractNarDepsTask(compileName, runtimeName, taskName, depsDir, tasks, project)
        binary.tasks.add(task)
        return task
    }

    private Task createExtractNarDepsTask(String compileName, String runtimeName, String taskName,
                                  File narDepsDir, TaskContainer tasks, Project project) {
        def task = tasks.findByName(taskName)
        if (task == null) {
            def compileConf = project.configurations[compileName]
            def runtimeConf = project.configurations[runtimeName]

            task = tasks.create(taskName, Copy, new Action<Copy>() {
                @Override
                public void execute(Copy copy) {
                    copy.group = NAR_GROUP
                    copy.description = "Extracts native artifact dependencies for configurations $compileConf and $runtimeConf."
                    copy.dependsOn compileConf, runtimeConf

                    copy.inputs.files compileConf
                    copy.inputs.files runtimeConf
                    copy.outputs.dir narDepsDir

                    copy.from {
                        def compileFiles = compileConf.findAll {
                            it.name.endsWith('.nar') || it.name.endsWith('.zip')
                        } as Set

                        def runtimeFiles = runtimeConf.findAll {
                            (it.name.endsWith('.nar') || it.name.endsWith('.zip')) && !compileFiles.contains(it)
                        } as Set

                        def compileTrees = compileFiles.collect {
                            copy.logger.debug("Extracting NAR file {}", it)
                            project.zipTree(it)
                        }
                        def runtimeTrees = runtimeFiles.collect {
                            copy.logger.debug("Extracting NAR file {}", it)
                            project.zipTree(it).matching {
                                include 'lib/**/*.a'
                                include 'lib/**/*.so'
                                include 'lib/**/*.lib'
                                include 'lib/**/*.dll'
                            }
                        }

                        return compileTrees + runtimeTrees
                    }
                    copy.into narDepsDir
                }
            })
        }

        return task
    }

    private Set<Task> getCompileTasks(NativeBinarySpec binary) {
        Set<Task> result = binary.tasks.findAll { it.name.startsWith('compile') }
        return result
    }
}
