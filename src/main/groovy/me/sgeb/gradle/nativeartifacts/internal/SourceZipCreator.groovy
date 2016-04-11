package me.sgeb.gradle.nativeartifacts.internal

import me.sgeb.gradle.nativeartifacts.NativeSoftwareComponent

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.DomainObjectContext
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.bundling.Zip
import org.gradle.internal.service.ServiceRegistry
import org.gradle.language.nativeplatform.HeaderExportingSourceSet
import org.gradle.model.Defaults
import org.gradle.model.ModelMap
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.NativeComponentSpec
import org.gradle.nativeplatform.NativeExecutableSpec
import org.gradle.nativeplatform.NativeLibrarySpec
import org.gradle.util.GUtil

class SourceZipCreator extends RuleSource {

    @SuppressWarnings("GroovyUnusedDeclaration")
    @Defaults
    void createSourceZipTasks(TaskContainer tasks, ModelMap<NativeComponentSpec> components, ServiceRegistry serviceRegistry) {
        Project project = serviceRegistry.get(DomainObjectContext)

        for (NativeComponentSpec component in components.values()) {
            if (targetedComponentType(component)) {
                def task = createSourcesZipTask(tasks, component, project)

                NativeSoftwareComponent nativeComponent = project.components.findByName(component.name)

                if (nativeComponent == null) {
                    nativeComponent = new NativeSoftwareComponent(component.name)
                    nativeComponent.from(component)
                    project.components.add(nativeComponent)
                }

                nativeComponent.from(task)
            }
        }
    }

    private boolean targetedComponentType(NativeComponentSpec nativeComponent) {
        return nativeComponent instanceof NativeExecutableSpec || nativeComponent instanceof NativeLibrarySpec
    }

    private Task createSourcesZipTask(TaskContainer tasks, final NativeComponentSpec component, final Project project) {
        String name = GUtil.toCamelCase(component.name)
        return tasks.create("create${name}SourcesZip", Zip, new Action<Zip>() {
            @Override
            public void execute(final Zip zip) {
                zip.description = "Create Zip archive with $component.name sources"
                zip.group = 'Build'
                zip.baseName = component.name
                zip.classifier = 'sources'
                zip.destinationDir = new File(project.buildDir, "distributions")

                for (sourceSet in component.sources) {
                    addCopySpecs(zip, project, sourceSet.source)
                    if (sourceSet instanceof HeaderExportingSourceSet) {
                        addCopySpecs(zip, project, sourceSet.exportedHeaders)
                    }
                }
            }
        })
    }

    private void addCopySpecs(Zip zip, Project project, sourceDirSet) {
        for (tree in sourceDirSet.srcDirTrees) {
            zip.into(project.rootProject.relativePath(tree.dir)) {
                from(tree.dir) {
                    include tree.patterns.includes
                    exclude tree.patterns.excludes
                }
            }
        }
    }
}
