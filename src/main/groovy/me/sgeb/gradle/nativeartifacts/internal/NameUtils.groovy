package me.sgeb.gradle.nativeartifacts.internal

import org.gradle.api.Named
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.SharedLibraryBinarySpec
import org.gradle.nativeplatform.StaticLibraryBinarySpec
import org.gradle.nativeplatform.internal.NativeBinarySpecInternal;
import org.gradle.platform.base.internal.DefaultBinaryNamingScheme

class NameUtils
{
    public static final String NAR_COMPILE_CONFIGURATION_PREFIX = 'compileNative'
    public static final String NAR_GROUP = 'Native Artifacts'
    public static final String NAR_EXTRACT_DEPS_TASK_PREFIX = 'extractNarDeps'
    public static final String NAR_EXTRACT_PATH = "nar-dependencies"

    static String getCompileConfigurationName(NativeBinarySpec binary) {
        getConfigurationNameVar(NAR_COMPILE_CONFIGURATION_PREFIX, binary.component, binary.targetPlatform,
                binary.buildType, binary.flavor)
    }

    static String getExtractNarDepsTaskName(NativeBinarySpecInternal binary) {
        return binary.namingScheme.getTaskName(NAR_EXTRACT_DEPS_TASK_PREFIX)
    }

    static String getNarDepsDirName(NativeBinarySpec binary) {
        // The directory name is designated only by the build variants and not
        // the component name. We assume that same dependencies from different
        // components will not introduce different contents. We can revisit making
        // this a choice if need be.
        def dirName = getConfigurationNameVar(NAR_COMPILE_CONFIGURATION_PREFIX,
                         binary.targetPlatform, binary.buildType, binary.flavor)
        return "$NAR_EXTRACT_PATH/$dirName"
    }

    static String getConfigurationNameVar(String prefix, Named... objects) {
        List params = new ArrayList()
        params.add prefix
        params.addAll(objects.grep { !(it.name in ["default", "current"]) }*.name)

        new DefaultBinaryNamingScheme(null, '', new ArrayList<String>()).makeName(params.toArray(new String[params.size()]))
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
