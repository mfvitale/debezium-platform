/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.domain;

import java.nio.file.Path;

import jakarta.enterprise.context.ApplicationScoped;

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

    @ConfigProperty(name = "conductor.descriptors.volume-source", defaultValue = "false")
    boolean volumeSource;

    @ConfigProperty(name = "conductor.descriptors.path")
    String ociArtifactExtractionPath;

    @ConfigProperty(name = "conductor.descriptors.image.registry")
    String ociArtifactRegistry;

    @ConfigProperty(name = "conductor.descriptors.image.name")
    String ociArtifactName;

    @ConfigProperty(name = "conductor.descriptors.image.tag")
    String ociArtifactTag;

    Registry registry;

    public OCIArtifactLoader(@OrasRegistry("registry") Registry registry) {
        this.registry = registry;
    }

    @Startup
    public void pullArtifacts() {

        if (volumeSource) {
            LOGGER.info("Descriptor volume source enabled, skipping ORAS download. Reading from: {}", ociArtifactExtractionPath);
            return;
        }

        String fullImageRef = String.format("%s:%s", ociArtifactName, ociArtifactTag);
        LOGGER.info("Downloading {} from {}", fullImageRef, ociArtifactRegistry);
        LOGGER.info("Manifest: {}", registry.getManifest(ContainerRef.parse(fullImageRef)).getJson());

        // Use absolute path to avoid normalization issues with tar entries like "./"
        Path targetDir = Path.of(ociArtifactExtractionPath).toAbsolutePath();
        registry.pullArtifact(ContainerRef.parse(fullImageRef), targetDir, true);
    }
}
