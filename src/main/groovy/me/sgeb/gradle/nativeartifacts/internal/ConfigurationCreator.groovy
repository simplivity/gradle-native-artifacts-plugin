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
    public void createConfigurationForComponent(
        NativeComponentSpec nativeComponent,
        PlatformResolvers platforms,
        BuildTypeContainer buildTypes,
        FlavorContainer flavors,
        ServiceRegistry serviceRegistry)
    {
        NativePlatforms nativePlatforms = serviceRegistry.get(NativePlatforms)
        ProjectInternal project = serviceRegistry.get(DomainObjectContext)

        if (targetedComponentType(nativeComponent)) {
            createConfigurationForComponentImpl(nativeComponent, platforms, buildTypes, flavors, nativePlatforms, project.configurations)
        }

        nativeComponent.binaries.all(new Action<NativeBinarySpec>() {
            @Override
            public void execute(NativeBinarySpec binary) {
                ConfigurationCreator.createConfigurationForBinary(binary, project.configurations)
                ConfigurationCreator.setNativeBinaryProperties(binary, project)
            }
        })
    }

    private static void createConfigurationForBinary(NativeBinarySpec binary, ConfigurationContainer configurations) {
        if (!targetedComponentType(binary.component)) {
            createConfigurationHierarchy(configurations, binary.component,
                binary.targetPlatform, binary.buildType, binary.flavor)
        }
    }

    private static void setNativeBinaryProperties(NativeBinarySpec binary, ProjectInternal project) {
        def depsDirName = getNarDepsDirName(binary)
        binary.ext.narDepsDir = project.file("$project.buildDir/${depsDirName}")
        binary.ext.narConfName = getCompileConfigurationName(binary)
    }

    static boolean targetedComponentType(NativeComponentSpec nativeComponent) {
        return nativeComponent instanceof NativeExecutableSpec || nativeComponent instanceof NativeLibrarySpec
    }

    private void createConfigurationForComponentImpl(
        NativeComponentSpec nativeComponent,
        PlatformResolvers platforms,
        BuildTypeContainer buildTypes,
        FlavorContainer flavors,
        NativePlatforms nativePlatforms,
        ConfigurationContainer configurations) {

        TargetedNativeComponentInternal targetedComponent = (TargetedNativeComponentInternal) nativeComponent
        List<NativePlatform> resolvedPlatforms = resolvePlatforms(targetedComponent, nativePlatforms, platforms)

        for (NativePlatform platform : resolvedPlatforms) {

            Set<BuildType> targetBuildTypes = targetedComponent.chooseBuildTypes(buildTypes)
            for (BuildType buildType : targetBuildTypes) {

                Set<Flavor> targetFlavors = targetedComponent.chooseFlavors(flavors)
                for (Flavor flavor : targetFlavors) {
                    createConfigurationHierarchy(configurations, nativeComponent, platform, buildType, flavor)
                }
            }
        }
    }

    private List<NativePlatform> resolvePlatforms(TargetedNativeComponentInternal targetedComponent, NativePlatforms nativePlatforms, final PlatformResolvers platforms) {
        List<PlatformRequirement> targetPlatforms = targetedComponent.getTargetPlatforms()
        if (targetPlatforms.isEmpty()) {
            PlatformRequirement requirement = DefaultPlatformRequirement.create(nativePlatforms.getDefaultPlatformName())
            targetPlatforms = Collections.singletonList(requirement)
        }
        return CollectionUtils.collect(targetPlatforms, new Transformer<NativePlatform, PlatformRequirement>() {
            @Override
            public NativePlatform transform(PlatformRequirement platformRequirement) {
                return platforms.resolve(NativePlatform.class, platformRequirement)
            }
        })
    }

    private static void createConfigurationHierarchy(ConfigurationContainer configurations, Named... objects)
    {
        for (int i = objects.size()-1; i >= 0; i--) {
            def parentParams = (i != 0) ? objects[0..i-1] : []
            def confParentName = getConfigurationNameVar(NAR_COMPILE_CONFIGURATION_PREFIX, parentParams as Named[])
            def confName = getConfigurationNameVar(NAR_COMPILE_CONFIGURATION_PREFIX, objects[0..i] as Named[])

            if (confName != confParentName) {
                def confParent = configurations.maybeCreate(confParentName)
                def conf = configurations.maybeCreate(confName)
                conf.extendsFrom confParent
            }
        }
    }
}
