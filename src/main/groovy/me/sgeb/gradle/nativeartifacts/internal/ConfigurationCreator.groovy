package me.sgeb.gradle.nativeartifacts.internal

import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.*

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.internal.DomainObjectContext
import org.gradle.internal.service.ServiceRegistry
import org.gradle.model.Defaults
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.NativeComponentSpec
import org.gradle.nativeplatform.test.NativeTestSuiteBinarySpec

class ConfigurationCreator extends RuleSource {

    @Defaults
    public void createConfigurationForComponent(NativeComponentSpec nativeComponent, ServiceRegistry serviceRegistry)
    {
        Project project = serviceRegistry.get(DomainObjectContext)

        nativeComponent.binaries.withType(NativeBinarySpec, new Action<NativeBinarySpec>() {
            @Override
            public void execute(NativeBinarySpec binary) {
                createConfigurationHierarchy(project.configurations,
                                NAR_COMPILE_CONFIGURATION_PREFIX, binary.component,
                                binary.targetPlatform, binary.buildType, binary.flavor)
                createConfigurationHierarchy(project.configurations,
                                NAR_RUNTIME_CONFIGURATION_PREFIX, binary.component,
                                binary.targetPlatform, binary.buildType, binary.flavor)
                if (binary instanceof NativeTestSuiteBinarySpec) {
                    createConfigurationHierarchy(project.configurations,
                                    NAR_TEST_COMPILE_CONFIGURATION_PREFIX, binary.component,
                                    binary.targetPlatform, binary.buildType, binary.flavor)
                    createConfigurationHierarchy(project.configurations,
                                    NAR_TEST_RUNTIME_CONFIGURATION_PREFIX, binary.component,
                                    binary.targetPlatform, binary.buildType, binary.flavor)
                }
            }
        })
    }

    /**
     * Create a configuration hierarchy with "compileNative" as the root, using
     * componentName/platform/buildType/flavor as the dimensions. For now we skip
     * the componentName from the scheme and may introduce it as a plugin configuration
     * option if it becomes useful.
     *
     * @param configurations the ConfigurationContainer to manipulate the collection
     * @param prefix the configuration prefix to use
     * @param parts the collection of componentName/platform/buildType/flavor dimensions
     */
    private void createConfigurationHierarchy(ConfigurationContainer configurations,
                        String prefix, Named... parts)
    {
        assert parts.size() == 4
        int start = 1 // for now we just skip the componentName part
        for (int i = parts.size()-1; i >= start; i--) {
            def parentParams = (i != start) ? parts[start..i-1] : []
            def confParentName = getConfigurationNameVar(prefix, parentParams as Named[])
            def confName = getConfigurationNameVar(prefix, parts[start..i] as Named[])

            if (confName != confParentName) {
                def confParent = configurations.maybeCreate(confParentName)
                def conf = configurations.maybeCreate(confName)
                conf.extendsFrom confParent
            }
        }
    }
}
