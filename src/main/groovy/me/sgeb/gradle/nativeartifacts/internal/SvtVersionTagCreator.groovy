package me.sgeb.gradle.nativeartifacts.internal

import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.NAR_GROUP
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.classifierForBinary

import org.gradle.api.Action
import org.gradle.api.tasks.AbstractExecTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskContainer
import org.gradle.internal.os.OperatingSystem
import org.gradle.model.Defaults
import org.gradle.model.ModelMap
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.NativeComponentSpec
import org.gradle.nativeplatform.NativeExecutableBinarySpec
import org.gradle.nativeplatform.NativeExecutableSpec
import org.gradle.nativeplatform.NativeLibrarySpec
import org.gradle.nativeplatform.SharedLibraryBinarySpec
import org.gradle.nativeplatform.StaticLibraryBinarySpec

class CreateVersionObjectTask extends AbstractExecTask<CreateVersionObjectTask> {
    CreateVersionObjectTask() {
        super(CreateVersionObjectTask)
    }

    @Input String versionTagText
    @OutputFile File versionTagObject
    @OutputFile File versionTagFile

    @TaskAction
    @Override
    protected void exec() {
        setExecutable('/usr/bin/ld')
        setWorkingDir(versionTagObject.parentFile)

        args '-r', '-b', 'binary'
        args '-o', versionTagObject.absolutePath
        args versionTagFile.absolutePath

        // create the input file on demand
        versionTagFile.text = versionTagText

        super.exec();
    }
}

class SvtVersionTagCreator extends RuleSource {
    public static final String CREATE_VERSION_TASK_PREFIX = 'createVersionTag'
    public static final String VERSION_TAG_PREFIX = 'VersionTag'

    @SuppressWarnings("GroovyUnusedDeclaration")
    @Defaults
    public void createVersionTagTasks(TaskContainer tasks, ModelMap<NativeBinarySpec> binaries) {

        if (OperatingSystem.current().linux) {
            binaries.each { NativeBinarySpec binary ->
                if (binary.buildable) {
                    NativeComponentSpec component = binary.getComponent()
                    if (targetedComponentType(component)) {

                        CreateVersionObjectTask task = createVersionTagTask(tasks, binary)

                        setTaskBuildDependency(task, binary)
                        binary.tasks.add(task)
                    }
                }
            }
        }
    }

    private boolean targetedComponentType(NativeComponentSpec nativeComponent) {
        return nativeComponent instanceof NativeExecutableSpec || nativeComponent instanceof NativeLibrarySpec
    }

    private CreateVersionObjectTask createVersionTagTask(TaskContainer tasks, NativeBinarySpec binary) {

        def versionTagTaskName = getVersionTagTaskName(binary)
        CreateVersionObjectTask versionTagTask = tasks.create(versionTagTaskName, CreateVersionObjectTask, new Action<CreateVersionObjectTask>() {
                @Override
                public void execute(CreateVersionObjectTask task) {
                    task.group = NAR_GROUP
                    task.description = "Creates version object tag for $binary.namingScheme.description."

                    def fileName = getVersionTagFileName(binary)
                    task.versionTagText = createVersionTag(task.project.group, task.project.version.toString(), task.project.gitdata.commit, binary)
                    task.versionTagFile = new File(task.project.buildDir, "version-tags/${fileName}.txt")
                    task.versionTagObject = new File(task.project.buildDir, "version-tags/${fileName}.o")
                }
            })

        return versionTagTask
    }

    private String createVersionTag(String group, String version, String gitCommit, NativeBinarySpec binary) {
        String name = binary.component.name
        String classifier = classifierForBinary(binary)

        return "\nSVT_MODULE_VERSION:$group:$name:$version:$classifier:$gitCommit\n"
    }

    private String getVersionTagTaskName(NativeBinarySpec binary) {
        return binary.namingScheme.getTaskName(CREATE_VERSION_TASK_PREFIX)
    }

    private String getVersionTagFileName(NativeBinarySpec binary) {
        return binary.namingScheme.getTaskName(VERSION_TAG_PREFIX)
    }

    private void setTaskBuildDependency(CreateVersionObjectTask versionTagTask, NativeBinarySpec binary) {
        if (binary instanceof NativeExecutableBinarySpec) {
            binary.tasks.link.dependsOn versionTagTask
            // Link the generated object to the library or executable
            binary.linker.args versionTagTask.versionTagObject.absolutePath
        }
        if (binary instanceof SharedLibraryBinarySpec) {
            binary.tasks.link.dependsOn versionTagTask
            // Link the generated object to the library or executable
            binary.linker.args versionTagTask.versionTagObject.absolutePath
        }
    }

}
