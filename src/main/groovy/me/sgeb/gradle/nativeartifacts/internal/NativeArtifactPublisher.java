package me.sgeb.gradle.nativeartifacts.internal;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.component.SoftwareComponentContainer;
import org.gradle.api.internal.DomainObjectContext;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.ivy.IvyPublication;
import org.gradle.api.publish.ivy.plugins.IvyPublishPlugin;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.model.Finalize;
import org.gradle.model.ModelMap;
import org.gradle.model.RuleSource;
import org.gradle.nativeplatform.NativeBinarySpec;

import me.sgeb.gradle.nativeartifacts.NativeSoftwareComponent;

public class NativeArtifactPublisher implements Plugin<Project> {
    private static final Logger logger = Logging.getLogger(NativeArtifactPublisher.class);

    @Override
    public void apply(final Project project)
    {
        project.getPluginManager().apply(IvyPublishPlugin.class);
    }

    static class Rules extends RuleSource {

        @Finalize
        void publishAllNativeSoftwareComponents(PublishingExtension publishing, ModelMap<NativeBinarySpec> binaries, ServiceRegistry serviceRegistry) {
            Project project = (Project) serviceRegistry.get(DomainObjectContext.class);

            if (project.hasProperty("disableNativePublishing")) {
                return;
            }

            SoftwareComponentContainer components = project.getComponents();

            for (final NativeSoftwareComponent component : components.withType(NativeSoftwareComponent.class)) {
                final String name = component.getName();
                logger.debug("Creating publication for native component {}", name);

                publishing.getPublications().create(name, IvyPublication.class, new Action<IvyPublication>() {

                    @Override
                    public void execute(IvyPublication publication)
                    {
                        publication.from(component);
                        publication.setModule(name);
                    }
                });
            }
        }
    }
}
