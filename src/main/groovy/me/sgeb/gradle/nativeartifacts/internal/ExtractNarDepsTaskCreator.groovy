package me.sgeb.gradle.nativeartifacts.internal

import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.NAR_GROUP
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.getCompileConfigurationName
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.getTestConfigurationName
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.getNarCompileDepsDir
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.getNarTestDepsDir
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.getExtractNarCompileDepsTaskName
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.getExtractNarTestDepsTaskName

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskContainer
import org.gradle.model.Finalize
import org.gradle.model.RuleSource
import org.gradle.model.ModelMap
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.test.NativeTestSuiteBinarySpec
import org.gradle.api.internal.DomainObjectContext
import org.gradle.internal.service.ServiceRegistry

class ExtractNarDepsTaskCreator extends RuleSource {

    @SuppressWarnings("GroovyUnusedDeclaration")
    @Finalize
    public void createExtractNarDepsTasks(TaskContainer tasks, ModelMap<NativeBinarySpec> binaries, ServiceRegistry serviceRegistry) {

        Project project = serviceRegistry.get(DomainObjectContext)

        binaries.each { NativeBinarySpec binary ->
            Task extractCompileDepsTask = createExtractCompileDepsTask(binary, tasks, project)
            binary.tasks.add(extractCompileDepsTask)

            Task extractTestDepsTask = null
            if (binary instanceof NativeTestSuiteBinarySpec) {
                extractTestDepsTask = createExtractTestDepsTask(binary, tasks, project)
                binary.tasks.add(extractTestDepsTask)
            }

            getCompileTasks(binary).each { compileTask ->
                compileTask.dependsOn(extractCompileDepsTask)
                if (extractTestDepsTask != null) {
                    compileTask.dependsOn(extractTestDepsTask)
                }
            }
        }
    }

    private Task createExtractCompileDepsTask(NativeBinarySpec binary, TaskContainer tasks, Project project) {
        String confName = getCompileConfigurationName(binary)
        String taskName = getExtractNarCompileDepsTaskName(binary)
        File depsDir = getNarCompileDepsDir(project.buildDir, binary)

        createExtractNarDepsTask(confName, taskName, depsDir, tasks, project)
    }

    private Task createExtractTestDepsTask(NativeBinarySpec binary, TaskContainer tasks, Project project) {
        String confName = getTestConfigurationName(binary)
        String taskName = getExtractNarTestDepsTaskName(binary)
        File depsDir = getNarTestDepsDir(project.buildDir, binary)

        createExtractNarDepsTask(confName, taskName, depsDir, tasks, project)
    }

    private Task createExtractNarDepsTask(String depsConfName, String extractDepsTaskName,
                                  File narDepsDir, TaskContainer tasks, Project project) {
        def depsConf = project.configurations[depsConfName]
        def extractDepsTask = tasks.findByName(extractDepsTaskName)
        if (extractDepsTask == null) {
            extractDepsTask = tasks.create(extractDepsTaskName, Copy, new Action<Copy>() {
                @Override
                public void execute(Copy copy) {
                    copy.group = NAR_GROUP
                    copy.description = "Extracts native artifact dependencies for configuration $depsConfName."
                    copy.dependsOn depsConf

                    copy.inputs.files depsConf
                    copy.outputs.dir narDepsDir

                    copy.from {
                        depsConf.findAll {
                            it.name.endsWith('.nar') || it.name.endsWith('.zip')
                        }.collect {
                            project.zipTree(it)
                        }
                    }
                    copy.into narDepsDir
                }
            })
        }

        return extractDepsTask
    }

    private Set<Task> getCompileTasks(NativeBinarySpec binary) {
        Set<Task> result = binary.tasks.findAll { it.name.startsWith('compile') }
        return result
    }
}
