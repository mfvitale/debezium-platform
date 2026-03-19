/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.domain;

import java.nio.file.Path;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkiverse.oras.runtime.OrasRegistry;
import io.quarkus.runtime.Startup;

import land.oras.ContainerRef;
import land.oras.Registry;

@ApplicationScoped
public class OCIArtifactLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(OCIArtifactLoader.class);

    @ConfigProperty(name = "conductor.descriptors.volume-source", defaultValue = "true")
    boolean volumeSource;

    @ConfigProperty(name = "conductor.descriptors.path", defaultValue = "/opt/descriptors")
    String ociArtifactExtractionPath;

    @ConfigProperty(name = "conductor.descriptors.image.registry", defaultValue = "quay.io")
    String ociArtifactRegistry;

    @ConfigProperty(name = "conductor.descriptors.image.name", defaultValue = "debezium/debezium-descriptors")
    String ociArtifactName;

    @ConfigProperty(name = "conductor.descriptors.image.tag", defaultValue = "nightly")
    String ociArtifactTag;

    @Inject
    @OrasRegistry("registry")
    Instance<Registry> registryInstance;

    @Startup
    public void pullArtifacts() {

        if (volumeSource) {
            LOGGER.info("Descriptor volume source enabled, skipping ORAS download. Reading from: {}", ociArtifactExtractionPath);
            return;
        }

        if (registryInstance.isUnsatisfied()) {
            LOGGER.warn("ORAS Registry not available, skipping descriptor download. Ensure ORAS is configured in your profile.");
            return;
        }

        try {
            Registry registry = registryInstance.get();
            String fullImageRef = String.format("%s:%s", ociArtifactName, ociArtifactTag);
            LOGGER.info("Downloading {} from {}", fullImageRef, ociArtifactRegistry);
            LOGGER.info("Manifest: {}", registry.getManifest(ContainerRef.parse(fullImageRef)).getJson());

            // Use absolute path to avoid normalization issues with tar entries like "./"
            Path targetDir = Path.of(ociArtifactExtractionPath).toAbsolutePath();
            registry.pullArtifact(ContainerRef.parse(fullImageRef), targetDir, true);

            LOGGER.info("Successfully downloaded descriptors to {}", targetDir);
        }
        catch (Exception e) {
            LOGGER.error("Failed to download descriptor OCI artifact from {}. " +
                    "Descriptor features may not be available. Error: {}",
                    ociArtifactRegistry, e.getMessage(), e);
        }
    }
}
