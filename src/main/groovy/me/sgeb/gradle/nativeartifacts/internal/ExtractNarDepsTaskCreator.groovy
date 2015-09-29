package me.sgeb.gradle.nativeartifacts.internal

import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.NAR_GROUP
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.getCompileConfigurationName
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.getConfigurationNameVar
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.getExtractNarDepsTaskName
import static org.apache.commons.lang.StringUtils.capitalize

import org.gradle.api.Task
import org.gradle.api.internal.DomainObjectContext
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskContainer
import org.gradle.internal.service.ServiceRegistry
import org.gradle.model.Finalize
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.internal.NativeBinarySpecInternal
import org.gradle.platform.base.BinaryContainer

class ExtractNarDepsTaskCreator extends RuleSource {

    @SuppressWarnings("GroovyUnusedDeclaration")
    @Finalize
    public void createExtractNarDepsTasks(TaskContainer tasks, BinaryContainer binaries, ServiceRegistry serviceRegistry) {

        ProjectInternal project = serviceRegistry.get(DomainObjectContext)
        binaries.withType(NativeBinarySpecInternal).all { NativeBinarySpecInternal binary ->
            Task extractTask = createExtractNarDepsTask(binary, tasks, project)
            getCompileTasks(binary).each { compileTask ->
                compileTask.dependsOn(extractTask)
            }
            binary.tasks.add(extractTask)
        }
    }

    private Task createExtractNarDepsTask(NativeBinarySpecInternal binary, TaskContainer tasks, ProjectInternal project) {
        def depsConfName = getCompileConfigurationName(binary)
        def depsConf = project.configurations[depsConfName]

        String extractDepsTaskName = getExtractNarDepsTaskName(binary)
        def extractDepsTask = tasks.create(extractDepsTaskName, Copy) {
            group = NAR_GROUP
            description = "Extracts native artifact dependencies for " +
                    "$binary.namingScheme.description."
            dependsOn depsConf

            inputs.files depsConf
            outputs.files { binary.narDepsDir.listFiles() }

            from {
                depsConf.findAll {
                    it.name.endsWith('.nar') || it.name.endsWith('.zip')
                }.collect {
                    project.zipTree(it)
                }
            }
            into binary.narDepsDir
        }

        return extractDepsTask
    }

    private Set<Task> getCompileTasks(NativeBinarySpecInternal binary) {
        Set<Task> result = binary.tasks.findAll { it.name.startsWith('compile') }
        return result
    }

}
