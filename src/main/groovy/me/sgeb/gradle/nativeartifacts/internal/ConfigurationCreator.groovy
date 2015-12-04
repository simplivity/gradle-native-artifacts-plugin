package me.sgeb.gradle.nativeartifacts.internal

import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.NAR_COMPILE_CONFIGURATION_PREFIX
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.getCompileConfigurationName
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.getConfigurationNameVar
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.getNarDepsDirName

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.internal.DomainObjectContext
import org.gradle.internal.service.ServiceRegistry
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.NativeComponentSpec
import org.gradle.nativeplatform.NativeExecutableSpec
import org.gradle.nativeplatform.NativeLibrarySpec
import org.gradle.model.Defaults
import org.gradle.model.RuleSource

class ConfigurationCreator extends RuleSource {

    @Defaults
    public void createConfigurationForComponent(NativeComponentSpec nativeComponent, ServiceRegistry serviceRegistry)
    {
        Project project = serviceRegistry.get(DomainObjectContext)

        nativeComponent.binaries.withType(NativeBinarySpec, new Action<NativeBinarySpec>() {
            @Override
            public void execute(NativeBinarySpec binary) {
                createConfigurationForBinary(binary, project.configurations)
                setNativeBinaryProperties(binary, project.buildDir)
            }
        })
    }

    private void createConfigurationForBinary(NativeBinarySpec binary, ConfigurationContainer configurations) {
        createConfigurationHierarchy(configurations, binary.component,
            binary.targetPlatform, binary.buildType, binary.flavor)
    }

    private void setNativeBinaryProperties(NativeBinarySpec binary, File buildDir) {
        def depsDirName = getNarDepsDirName(binary)
        binary.ext.narDepsDir = new File(buildDir, depsDirName)
        binary.ext.narConfName = getCompileConfigurationName(binary)
    }

    /**
     * Create a configuration hierarchy with "compileNative" as the root, using
     * componentName/platform/buildType/flavor as the dimensions. For now we skip
     * the componentName from the scheme and may introduce it as a plugin configuration
     * option if it becomes useful.
     *
     * @param configurations the ConfigurationContainer to manipulate the collection
     * @param parts the collection of componentName/platform/buildType/flavor dimensions
     */
    private void createConfigurationHierarchy(ConfigurationContainer configurations, Named... parts)
    {
        assert parts.size() == 4
        int start = 1 // for now we just skip the componentName part
        for (int i = parts.size()-1; i >= start; i--) {
            def parentParams = (i != start) ? parts[start..i-1] : []
            def confParentName = getConfigurationNameVar(NAR_COMPILE_CONFIGURATION_PREFIX, parentParams as Named[])
            def confName = getConfigurationNameVar(NAR_COMPILE_CONFIGURATION_PREFIX, parts[start..i] as Named[])

            if (confName != confParentName) {
                def confParent = configurations.maybeCreate(confParentName)
                def conf = configurations.maybeCreate(confName)
                conf.extendsFrom confParent
            }
        }
    }
}
