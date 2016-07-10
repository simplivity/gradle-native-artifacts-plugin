package me.sgeb.gradle.nativeartifacts.internal

import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.*

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileTree
import org.gradle.api.internal.DomainObjectContext
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.specs.Specs
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskContainer
import org.gradle.internal.service.ServiceRegistry
import org.gradle.model.Finalize
import org.gradle.model.ModelMap
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.NativeBinarySpec

interface DependencyTreeResolver {
    Configuration compileConf()
    Set<File> resolvedFiles()
    Collection<FileTree> resolvedFileTrees()
}

class CompileDependencyTreeResolver implements DependencyTreeResolver {
    private static final Logger logger = Logging.getLogger(CompileDependencyTreeResolver.class)

    private final Project project
    private final Configuration compile

    CompileDependencyTreeResolver(Project project, String compileName) {
        this.project = project
        compile = project.configurations[compileName]
    }

    Configuration compileConf() {
        return compile
    }

    Set<File> resolvedFiles() {
        compile.findAll {
            it.name.endsWith('.nar') || it.name.endsWith('.zip')
        } as Set
    }

    Collection<FileTree> resolvedFileTrees() {
        resolvedFiles().collect {
            logger.debug("Extracting NAR file {}", it)
            project.zipTree(it)
        }
    }
}

class TestCompileDependencyTreeResolver implements DependencyTreeResolver {
    private static final Logger logger = Logging.getLogger(TestCompileDependencyTreeResolver.class)

    private final Project project
    private final Configuration testCompile
    private final DependencyTreeResolver compileResolver

    TestCompileDependencyTreeResolver(DependencyTreeResolver compileResolver, Project project, String testCompileName) {
        this.compileResolver = compileResolver
        this.project = project
        testCompile = project.configurations[testCompileName]
    }

    Configuration compileConf() {
        return testCompile
    }

    Set<File> resolvedFiles() {
        def compileFiles = testCompile.findAll {
            it.name.endsWith('.nar') || it.name.endsWith('.zip')
        } as Set

        return compileFiles - compileResolver.resolvedFiles()
    }

    Collection<FileTree> resolvedFileTrees() {
        resolvedFiles().collect {
            logger.debug("Extracting NAR file {}", it)
            project.zipTree(it)
        }
    }
}

class ExtractNarDepsTaskCreator extends RuleSource {
    @SuppressWarnings("GroovyUnusedDeclaration")
    @Finalize
    public void createExtractNarDependenciesTasks(TaskContainer tasks, ModelMap<NativeBinarySpec> binaries, final NativeDependencyContainer dependencies, ServiceRegistry serviceRegistry) {

        Project project = serviceRegistry.get(DomainObjectContext)

        for (NativeBinarySpec binary : binaries.values()) {
            Task extractCompileDepsTask = createExtractCompileDepsTask(binary, tasks, project)

            getCompileTasks(binary).each { compileTask ->
                compileTask.dependsOn(extractCompileDepsTask)
            }
        }
    }

    private Task createExtractCompileDepsTask(NativeBinarySpec binary, TaskContainer tasks, Project project) {
        String taskName = getExtractNarCompileDepsTaskName(binary)
        Task task = tasks.findByName(taskName)
        if (task == null) {
            String compileName = getCompileConfigurationName(binary)
            String testCompileName = getTestCompileConfigurationName(binary)
            File depsDir = getNarCompileDepsDir(project.buildDir, binary)

            DependencyTreeResolver compileResolver = new CompileDependencyTreeResolver(project, compileName)
            DependencyTreeResolver testResolver = new TestCompileDependencyTreeResolver(compileResolver, project, testCompileName)

            task = createExtractNarDepsTask(compileResolver, testResolver, taskName, depsDir, tasks)
            binary.tasks.add(task)
        }
        return task
    }

    private Task createExtractNarDepsTask(DependencyTreeResolver compileResolver,
                                          DependencyTreeResolver testResolver,
                                          String taskName, File narDepsDir, TaskContainer tasks) {

        return tasks.create(taskName, Copy, new Action<Copy>() {
            @Override
            public void execute(Copy copy) {
                copy.group = NAR_GROUP
                copy.dependsOn compileResolver.compileConf(), testResolver.compileConf()
                copy.description = "Extracts native artifact dependencies for configurations ${compileResolver.compileConf().name} and ${testResolver.compileConf().name}"

                copy.inputs.files(compileResolver.compileConf().fileCollection(Specs.satisfyAll()))
                copy.inputs.files(testResolver.compileConf().fileCollection(Specs.satisfyAll()))

                copy.into narDepsDir

                copy.into('main') {
                    from {
                        compileResolver.resolvedFileTrees()
                    }
                    include 'include/**'
                }
                copy.into('test') {
                    from {
                        testResolver.resolvedFileTrees()
                    }
                    include 'include/**'
                }
                copy.into('.') {
                    from {
                        compileResolver.resolvedFileTrees()
                    }
                    from {
                        testResolver.resolvedFileTrees()
                    }
                    include 'lib/**'
                }
            }
        })
    }

    private Set<Task> getCompileTasks(NativeBinarySpec binary) {
        Set<Task> result = binary.tasks.findAll { it.name.startsWith('compile') }
        return result
    }
}
