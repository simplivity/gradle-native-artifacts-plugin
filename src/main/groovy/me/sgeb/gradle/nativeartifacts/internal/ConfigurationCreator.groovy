package me.sgeb.gradle.nativeartifacts.internal

import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.NAR_COMPILE_CONFIGURATION_PREFIX
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.classifierForBinary
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.getCompileConfigurationName
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.getConfigurationNameVar
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.getNarDepsDirName

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.Transformer
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.internal.DomainObjectContext
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.service.ServiceRegistry
import org.gradle.model.Defaults
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.BuildType
import org.gradle.nativeplatform.BuildTypeContainer
import org.gradle.nativeplatform.Flavor
import org.gradle.nativeplatform.FlavorContainer
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.NativeComponentSpec
import org.gradle.nativeplatform.NativeExecutableSpec
import org.gradle.nativeplatform.NativeLibrarySpec
import org.gradle.nativeplatform.internal.TargetedNativeComponentInternal
import org.gradle.nativeplatform.platform.NativePlatform
import org.gradle.nativeplatform.platform.internal.NativePlatforms
import org.gradle.platform.base.internal.DefaultPlatformRequirement
import org.gradle.platform.base.internal.PlatformRequirement
import org.gradle.platform.base.internal.PlatformResolvers
import org.gradle.util.CollectionUtils

class ConfigurationCreator extends RuleSource {

    @Defaults
    public void createConfigurationForComponent(NativeComponentSpec nativeComponent, ServiceRegistry serviceRegistry)
    {
        ProjectInternal project = serviceRegistry.get(DomainObjectContext)

        nativeComponent.binaries.all(new Action<NativeBinarySpec>() {
            @Override
            public void execute(NativeBinarySpec binary) {
                ConfigurationCreator.createConfigurationForBinary(binary, project.configurations)
                ConfigurationCreator.setNativeBinaryProperties(binary, project)
            }
        })
    }

    private static void createConfigurationForBinary(NativeBinarySpec binary, ConfigurationContainer configurations) {
        createConfigurationHierarchy(configurations, binary.component,
            binary.targetPlatform, binary.buildType, binary.flavor)
    }

    private static void setNativeBinaryProperties(NativeBinarySpec binary, ProjectInternal project) {
        def depsDirName = getNarDepsDirName(binary)
        binary.ext.narDepsDir = project.file("$project.buildDir/${depsDirName}")
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
    private static void createConfigurationHierarchy(ConfigurationContainer configurations, Named... parts)
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
