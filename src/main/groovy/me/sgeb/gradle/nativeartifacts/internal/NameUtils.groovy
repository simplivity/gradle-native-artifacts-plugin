package me.sgeb.gradle.nativeartifacts.internal

import org.gradle.api.Named
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.SharedLibraryBinarySpec
import org.gradle.nativeplatform.StaticLibraryBinarySpec
import org.gradle.nativeplatform.internal.NativeBinarySpecInternal;
import org.gradle.platform.base.internal.DefaultBinaryNamingScheme

class NamedWrapper implements Named {
    private final String name;
    NamedWrapper(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

class NameUtils
{
    public static final String NAR_COMPILE_CONFIGURATION_PREFIX = 'compileNative'
    public static final String NAR_RUNTIME_CONFIGURATION_PREFIX = 'runtimeNative'
    public static final String NAR_TEST_COMPILE_CONFIGURATION_PREFIX = 'testCompileNative'
    public static final String NAR_TEST_RUNTIME_CONFIGURATION_PREFIX = 'testRuntimeNative'
    public static final String NAR_GROUP = 'Native Artifacts'
    public static final String NAR_EXTRACT_COMPILE_DEPS_TASK_PREFIX = 'extractNarCompileDeps'
    public static final String NAR_EXTRACT_TEST_COMPILE_DEPS_TASK_PREFIX = 'extractNarTestCompileDeps'
    public static final String NAR_EXTRACT_RUNTIME_DEPS_TASK_PREFIX = 'extractNarRuntimeDeps'
    public static final String NAR_EXTRACT_TEST_RUNTIME_DEPS_TASK_PREFIX = 'extractNarTestRuntimeDeps'
    public static final String NAR_EXTRACT_PATH = "nar-dependencies"

    static String getCompileConfigurationName(NativeBinarySpec binary) {
        getConfigurationNameVar(NAR_COMPILE_CONFIGURATION_PREFIX,
                binary.targetPlatform, binary.buildType, binary.flavor)
    }

    static String getRuntimeConfigurationName(NativeBinarySpec binary) {
        getConfigurationNameVar(NAR_RUNTIME_CONFIGURATION_PREFIX,
                binary.targetPlatform, binary.buildType, binary.flavor)
    }

    static String getRuntimeConfigurationName(String targetPlatform, String buildType, String flavor) {
        getConfigurationNameVar(NAR_RUNTIME_CONFIGURATION_PREFIX,
                new NamedWrapper(targetPlatform), new NamedWrapper(buildType), new NamedWrapper(flavor))
    }

    static String getTestCompileConfigurationName(NativeBinarySpec binary) {
        // We skip the componentName in this scheme (binary.component)
        getConfigurationNameVar(NAR_TEST_COMPILE_CONFIGURATION_PREFIX,
                binary.targetPlatform, binary.buildType, binary.flavor)
    }

    static String getTestRuntimeConfigurationName(NativeBinarySpec binary) {
        // We skip the componentName in this scheme (binary.component)
        getConfigurationNameVar(NAR_TEST_RUNTIME_CONFIGURATION_PREFIX,
                binary.targetPlatform, binary.buildType, binary.flavor)
    }

    static String getExtractNarCompileDepsTaskName(NativeBinarySpecInternal binary) {
        getConfigurationNameVar(NAR_EXTRACT_COMPILE_DEPS_TASK_PREFIX,
                binary.targetPlatform, binary.buildType, binary.flavor)
    }

    static String getExtractNarRuntimeDepsTaskName(NativeBinarySpecInternal binary) {
        getConfigurationNameVar(NAR_EXTRACT_RUNTIME_DEPS_TASK_PREFIX,
                binary.targetPlatform, binary.buildType, binary.flavor)
    }

    static String getExtractNarTestCompileDepsTaskName(NativeBinarySpecInternal binary) {
        getConfigurationNameVar(NAR_EXTRACT_TEST_COMPILE_DEPS_TASK_PREFIX,
                binary.targetPlatform, binary.buildType, binary.flavor)
    }

    static String getExtractNarTestRuntimeDepsTaskName(NativeBinarySpecInternal binary) {
        getConfigurationNameVar(NAR_EXTRACT_TEST_RUNTIME_DEPS_TASK_PREFIX,
                binary.targetPlatform, binary.buildType, binary.flavor)
    }

    static File getNarCompileDepsDir(File buildDir, NativeBinarySpec binary) {
        getNarDepsDir(buildDir, NAR_COMPILE_CONFIGURATION_PREFIX, binary)
    }

    static File getNarRuntimeDepsDir(File buildDir, NativeBinarySpec binary) {
        getNarDepsDir(buildDir, NAR_RUNTIME_CONFIGURATION_PREFIX, binary)
    }

    static File getNarTestCompileDepsDir(File buildDir, NativeBinarySpec binary) {
        getNarDepsDir(buildDir, NAR_TEST_COMPILE_CONFIGURATION_PREFIX, binary)
    }

    static File getNarTestRuntimeDepsDir(File buildDir, NativeBinarySpec binary) {
        getNarDepsDir(buildDir, NAR_TEST_RUNTIME_CONFIGURATION_PREFIX, binary)
    }

    static File getNarDepsDir(File buildDir, String prefix, NativeBinarySpec binary) {
        // The directory name is designated only by the build variants and not
        // the component name. We assume that same dependencies from different
        // components will not introduce different contents. We can revisit making
        // this a choice if need be.
        def dirName = getConfigurationNameVar(prefix,
                 binary.targetPlatform, binary.buildType, binary.flavor)
        new File(buildDir, "$NAR_EXTRACT_PATH/$dirName")
    }

    static String getConfigurationNameVar(String prefix, Named... objects) {
        List params = new ArrayList()
        params.add prefix
        params.addAll(objects.grep { !(it.name in ["default", "current"]) }*.name)

        DefaultBinaryNamingScheme.component(null).makeName(params.toArray(new String[params.size()]))
    }

    static String classifierForBinary(NativeBinarySpec binary) {
        return stringIfNotDefault(binary.targetPlatform.name, '') +
                libraryType(binary, '-') +
                stringIfNotDefault(binary.buildType.name) +
                stringIfNotDefault(binary.flavor.name)
    }

    private static String libraryType(NativeBinarySpec binary, String prefix = '-') {
        if (binary instanceof StaticLibraryBinarySpec) {
            return prefix + 'static'
        }
        if (binary instanceof SharedLibraryBinarySpec) {
            return prefix + 'shared'
        }
        return ''
    }

    private static String stringIfNotDefault(String str, String prefix = '-') {
        str != 'default' ? prefix + str : ''
    }
}
