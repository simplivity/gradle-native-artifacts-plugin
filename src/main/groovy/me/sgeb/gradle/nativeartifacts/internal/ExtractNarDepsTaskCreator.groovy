package me.sgeb.gradle.nativeartifacts.internal

import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.NAR_GROUP
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.getCompileConfigurationName
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.getExtractNarDepsTaskName

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskContainer
import org.gradle.model.Finalize
import org.gradle.model.RuleSource
import org.gradle.model.ModelMap
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.api.internal.DomainObjectContext
import org.gradle.internal.service.ServiceRegistry

class ExtractNarDepsTaskCreator extends RuleSource {

    @SuppressWarnings("GroovyUnusedDeclaration")
    @Finalize
    public void createExtractNarDepsTasks(TaskContainer tasks, ModelMap<NativeBinarySpec> binaries, ServiceRegistry serviceRegistry) {

        Project project = serviceRegistry.get(DomainObjectContext)

        binaries.each { NativeBinarySpec binary ->
            Task extractTask = createExtractNarDepsTask(binary, tasks, project)
            getCompileTasks(binary).each { compileTask ->
                compileTask.dependsOn(extractTask)
            }
            binary.tasks.add(extractTask)
        }
    }

    private Task createExtractNarDepsTask(NativeBinarySpec binary, TaskContainer tasks, Project project) {
        def depsConfName = getCompileConfigurationName(binary)
        def depsConf = project.configurations[depsConfName]

        def extractDepsTaskName = getExtractNarDepsTaskName(binary)
        def extractDepsTask = tasks.findByName(extractDepsTaskName)
        if (extractDepsTask == null) {
            extractDepsTask = tasks.create(extractDepsTaskName, Copy, new Action<Copy>() {
                @Override
                public void execute(Copy copy) {
                    copy.group = NAR_GROUP
                    copy.description = "Extracts native artifact dependencies for configuration $depsConfName."
                    copy.dependsOn depsConf

                    copy.inputs.files depsConf
                    copy.outputs.files { binary.narDepsDir.listFiles() }

                    copy.from {
                        depsConf.findAll {
                            it.name.endsWith('.nar') || it.name.endsWith('.zip')
                        }.collect {
                            project.zipTree(it)
                        }
                    }
                    copy.into binary.narDepsDir
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
