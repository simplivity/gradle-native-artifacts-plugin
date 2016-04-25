package me.sgeb.gradle.nativeartifacts.internal;

import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.getCompileConfigurationName;
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.getNarCompileDepsDir;
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.getNarTestCompileDepsDir;
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.getRuntimeConfigurationName;
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.getTestCompileConfigurationName;
import static me.sgeb.gradle.nativeartifacts.internal.NameUtils.getTestRuntimeConfigurationName;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.internal.DomainObjectContext;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.model.Finalize;
import org.gradle.model.Model;
import org.gradle.model.ModelMap;
import org.gradle.model.Mutate;
import org.gradle.model.RuleSource;
import org.gradle.nativeplatform.NativeBinarySpec;
import org.gradle.nativeplatform.test.NativeTestSuiteBinarySpec;

public class NativeDependenciesRules extends RuleSource {
    private static final Logger logger = Logging.getLogger(NativeDependenciesRules.class);

    @Model
    public NativeDependencyContainer nativeDependencies() {
        return new NativeDependencyContainer();
    }

    @Mutate
    public void addDependenciesForAllBinaries(ModelMap<NativeBinarySpec> binaries, final NativeDependencyContainer dependencies, final ServiceRegistry serviceRegistry) {
        Project project = (Project) serviceRegistry.get(DomainObjectContext.class);
        final DependencyHandler dependencyHandler = project.getDependencies();

        binaries.withType(NativeBinarySpec.class, new Action<NativeBinarySpec>() {

            @Override
            public void execute(NativeBinarySpec binary) {
                final String sharedClassifier = binary.getTargetPlatform().getName() + "-shared-" + binary.getBuildType().getName();
                final String staticClassifier = binary.getTargetPlatform().getName() + "-static-" + binary.getBuildType().getName();

                String compileName = getCompileConfigurationName(binary);
                String runtimeName = getRuntimeConfigurationName(binary);

                for (Map<String, String> downloadedLibrary : dependencies.getDownloadedCompileLibraries()) {
                    addLibraryDependency(dependencyHandler, compileName, downloadedLibrary, sharedClassifier, staticClassifier);
                    addTransitiveLibraryDependency(dependencyHandler, runtimeName, downloadedLibrary, sharedClassifier, staticClassifier);
                }
                for (Map<String, String> downloadedLibrary : dependencies.getDownloadedRuntimeLibraries()) {
                    addLibraryDependency(dependencyHandler, runtimeName, downloadedLibrary, sharedClassifier, staticClassifier);
                }
            }

        });
    }

    @Mutate
    public void addDependenciesForTestBinaries(ModelMap<NativeTestSuiteBinarySpec> binaries, final NativeDependencyContainer dependencies, final ServiceRegistry serviceRegistry) {
        Project project = (Project) serviceRegistry.get(DomainObjectContext.class);
        final DependencyHandler dependencyHandler = project.getDependencies();

        binaries.withType(NativeTestSuiteBinarySpec.class, new Action<NativeTestSuiteBinarySpec>() {

            @Override
            public void execute(NativeTestSuiteBinarySpec binary) {
                final String sharedClassifier = binary.getTargetPlatform().getName() + "-shared-" + binary.getBuildType().getName();
                final String staticClassifier = binary.getTargetPlatform().getName() + "-static-" + binary.getBuildType().getName();

                String compileName = getTestCompileConfigurationName(binary);
                String runtimeName = getTestRuntimeConfigurationName(binary);

                for (Map<String, String> downloadedLibrary : dependencies.getDownloadedTestCompileLibraries()) {
                    addLibraryDependency(dependencyHandler, compileName, downloadedLibrary, sharedClassifier, staticClassifier);
                    addTransitiveLibraryDependency(dependencyHandler, runtimeName, downloadedLibrary, sharedClassifier, staticClassifier);
                }
                for (Map<String, String> downloadedLibrary : dependencies.getDownloadedTestRuntimeLibraries()) {
                    addLibraryDependency(dependencyHandler, runtimeName, downloadedLibrary, sharedClassifier, staticClassifier);
                }
            }

        });
    }

    @Finalize
    public void addLibrariesToAllBinaries(ModelMap<NativeBinarySpec> binaries, final NativeDependencyContainer dependencies, final ServiceRegistry serviceRegistry) {
        final Project project = (Project) serviceRegistry.get(DomainObjectContext.class);

        binaries.withType(NativeBinarySpec.class, new Action<NativeBinarySpec>() {

            @Override
            public void execute(NativeBinarySpec binary) {
                for (Map<String, String> library : dependencies.getCompileLibraries()) {
                    binary.lib(library);
                }

                for (Map<String, String> library : dependencies.getRuntimeLibraries()) {
                    binary.lib(library);
                }

                File compileDir = getNarCompileDepsDir(project.getBuildDir(), binary);
                for (Map<String, String> downloadedLibrary : dependencies.getDownloadedCompileLibraries()) {
                    setDownloadedLibrary(binary, compileDir, downloadedLibrary);
                }

                for (Map<String, String> downloadedLibrary : dependencies.getDownloadedRuntimeLibraries()) {
                    setDownloadedLibrary(binary, compileDir, downloadedLibrary);
                }

                // Add the lib path to facilitate resolving transitive library
                // dependencies that are referenced by the direct dependencies.
                binary.getLinker().args("-L");
                binary.getLinker().args(new File(compileDir, "lib").getAbsolutePath());
            }

        });
    }

    @Finalize
    public void addLibrariesToTestBinaries(ModelMap<NativeTestSuiteBinarySpec> binaries, final NativeDependencyContainer dependencies, final ServiceRegistry serviceRegistry) {
        final Project project = (Project) serviceRegistry.get(DomainObjectContext.class);

        binaries.withType(NativeTestSuiteBinarySpec.class, new Action<NativeTestSuiteBinarySpec>() {

            @Override
            public void execute(NativeTestSuiteBinarySpec binary) {
                for (Map<String, String> library : dependencies.getTestCompileLibraries()) {
                    binary.lib(library);
                }

                for (Map<String, String> library : dependencies.getTestRuntimeLibraries()) {
                    binary.lib(library);
                }

                File compileDir = getNarTestCompileDepsDir(project.getBuildDir(), binary);
                for (Map<String, String> downloadedLibrary : dependencies.getDownloadedTestCompileLibraries()) {
                    setDownloadedLibrary(binary, compileDir, downloadedLibrary);
                }

                for (Map<String, String> downloadedLibrary : dependencies.getDownloadedTestRuntimeLibraries()) {
                    setDownloadedLibrary(binary, compileDir, downloadedLibrary);
                }

                // Add the lib path to facilitate resolving transitive library
                // dependencies that are referenced by the direct dependencies.
                binary.getLinker().args("-L");
                binary.getLinker().args(new File(compileDir, "lib").getAbsolutePath());
            }

        });
    }

    private void addLibraryDependency(DependencyHandler dependencies,
            String configuration, Map<String, String> lib, String sharedClassifier,
            String staticClassifier)
    {
        String classifier = selectClassifier(lib, sharedClassifier, staticClassifier);
        Map<String, String> dependency = new HashMap<String, String>();

        dependency.put("group",   requiredAttribute(lib, "group"));
        dependency.put("name",    requiredAttribute(lib, "library"));
        dependency.put("version", requiredAttribute(lib, "version"));

        dependency.put("ext",        optionalAttribute(lib, "ext", "nar"));

        classifier = optionalAttribute(lib, "classifier", classifier);
        dependency.put("classifier", classifier);
        dependency.put("configuration", classifier);

        logger.debug("Adding %s dependency: %s", configuration, dependency);
        dependencies.add(configuration, dependency);
    }

    private void addTransitiveLibraryDependency(DependencyHandler dependencies,
            String configuration, Map<String, String> lib, String sharedClassifier,
            String staticClassifier)
    {
        String classifier = selectClassifier(lib, sharedClassifier, staticClassifier);
        Map<String, String> dependency = new HashMap<String, String>();

        dependency.put("group",   requiredAttribute(lib, "group"));
        dependency.put("name",    requiredAttribute(lib, "library"));
        dependency.put("version", requiredAttribute(lib, "version"));
        dependency.put("configuration", classifier);

        logger.debug("Adding %s transitive dependency: %s", configuration, dependency);
        dependencies.add(configuration, dependency);
    }

    private String requiredAttribute(Map<String, String> lib, String attribute) {
        String value = lib.get(attribute);
        if (value == null) {
            throw new RuntimeException("Required downloaded library attribute " + attribute + " not specified");
        }

        return value;
    }

    private String optionalAttribute(Map<String, String> lib, String attribute, String defVal) {
        String value = lib.get(attribute);
        if (value == null) {
            return defVal;
        }

        return value;
    }

    private String selectClassifier(Map<String, String> downloadedLibrary, String sharedClassifier,
            String staticClassifier)
    {
        String linkage = downloadedLibrary.get("linkage");

        if ("static".equals(linkage)) {
            return staticClassifier;
        }

        if ("shared".equals(linkage)) {
            return sharedClassifier;
        }

        throw new RuntimeException("Unsupported downloaded library linkage " + linkage);
    }

    // Define a NativeDependencySet for a downloaded library capturing the include
    // path and library path. Currently this takes the following arguments:
    // binary:  the NativeBinarySpec that we are adding a dependency for
    // lib:     the downloaded library map notation
    private void setDownloadedLibrary(NativeBinarySpec binary, File narDepsDir, Map<String, String> lib) {
        String linkage = selectLinkage(lib);
        String name = requiredAttribute(lib, "library");

        setDownloadedLibrary(binary, narDepsDir, name, linkage);
    }

    private void setDownloadedLibrary(NativeBinarySpec binary, File narDepsDir, String name, String linkage) {
        File includePath = new File(narDepsDir, "include");
        File libPath = new File(narDepsDir, "lib");
        File libraryFile, linkLibraryFile;

        if (linkage.equals("shared")) {
            libraryFile = sharedLibrary(binary, libPath, name);
            linkLibraryFile = linkLibrary(binary, libPath, name);
            binary.lib(new DownloadedNativeDependencySet(includePath, linkLibraryFile, libraryFile));
        } else if (linkage.equals("static")) {
            libraryFile = staticLibrary(binary, libPath, name);
            linkLibraryFile = libraryFile;
            binary.lib(new DownloadedNativeDependencySet(includePath, linkLibraryFile, libraryFile));
        } else if (linkage.equals("api")) {
            binary.lib(new DownloadedNativeDependencySet(includePath));
        } else {
            throw new RuntimeException("Unsupported linkage type $linkage");
        }
    }

    private String selectLinkage(Map<String, String> downloadedLibrary) {
        String type = downloadedLibrary.get("type");
        if (type != null) {
            return type;
        }

        return requiredAttribute(downloadedLibrary, "linkage");
    }

    private File sharedLibrary(NativeBinarySpec binary, File libPath, String libName) {
        String fmt = binary.getTargetPlatform().getOperatingSystem().isWindows() ? "%1$s.dll" : "lib%1$s.so";

        return new File(libPath, String.format(fmt, libName));
    }

    private File staticLibrary(NativeBinarySpec binary, File libPath, String libName) {
        String fmt = binary.getTargetPlatform().getOperatingSystem().isWindows() ? "%1$s.lib" : "lib%1$s.a";

        return new File(libPath, String.format(fmt, libName));
    }

    private File linkLibrary(NativeBinarySpec binary, File libPath, String libName) {
        String fmt = binary.getTargetPlatform().getOperatingSystem().isWindows() ? "%1$s.lib" : "lib%1$s.so";

        return new File(libPath, String.format(fmt, libName));
    }

}
