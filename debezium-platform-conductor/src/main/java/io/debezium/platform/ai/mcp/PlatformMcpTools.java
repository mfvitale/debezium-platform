/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.ai.mcp;

import java.util.HashSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import com.blazebit.persistence.view.EntityViewManager;

import io.debezium.common.annotation.Incubating;
import io.debezium.platform.ai.dto.AiDestination;
import io.debezium.platform.ai.dto.AiPipeline;
import io.debezium.platform.ai.dto.AiSource;
import io.debezium.platform.domain.DestinationService;
import io.debezium.platform.domain.PipelineService;
import io.debezium.platform.domain.SourceService;
import io.debezium.platform.domain.views.Destination;
import io.debezium.platform.domain.views.Pipeline;
import io.debezium.platform.domain.views.Source;
import io.debezium.platform.domain.views.refs.DestinationReference;
import io.debezium.platform.domain.views.refs.SourceReference;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;

/**
 * MCP tools for Platform operations using direct service injection.
 * Exposes sources, destinations, and pipelines management to AI assistants.
 *
 * @author Mario Fiore Vitale
 */
@Incubating
@ApplicationScoped
public class PlatformMcpTools {

    @Inject
    SourceService sourceService;

    @Inject
    DestinationService destinationService;

    @Inject
    PipelineService pipelineService;

    @Inject
    EntityViewManager evm;

    @Inject
    io.debezium.platform.domain.VaultService vaultService;

    @Tool(description = "Get defined pipelines on Debezium Platform.")
    public String getPipelines() {
        var pipelines = pipelineService.list();

        if (pipelines == null || pipelines.isEmpty()) {
            return "No pipelines are currently defined on the Debezium Platform.";
        }

        return pipelines.stream().map(p -> """
                Name: %s
                Description: %s
                Source: %s
                Destination: %s
                """.formatted(
                p.getName(),
                p.getDescription() != null ? p.getDescription() : "(no description)",
                p.getSource() != null ? p.getSource().getName() : "(not set)",
                p.getDestination() != null ? p.getDestination().getName() : "(not set)"))
                .collect(Collectors.joining("\n---\n"));
    }

    @Tool(description = "Get defined sources on Debezium Platform.")
    public String getSources() {
        var sources = sourceService.list();

        if (sources == null || sources.isEmpty()) {
            return "No sources are currently defined on the Debezium Platform.";
        }

        return sources.stream().map(s -> """
                Name: %s
                Description: %s
                Id: %s
                Type: %s
                Schema: %s
                """.formatted(
                s.getName(),
                s.getDescription() != null ? s.getDescription() : "(no description)",
                s.getId(),
                s.getType(),
                s.getSchema()))
                .collect(Collectors.joining("\n---\n"));
    }

    @Tool(description = "Get defined destinations on Debezium Platform.")
    public String getDestinations() {
        var destinations = destinationService.list();

        if (destinations == null || destinations.isEmpty()) {
            return "No destinations are currently defined on the Debezium Platform.";
        }

        return destinations.stream().map(d -> """
                Name: %s
                Description: %s
                Id: %s
                Type: %s
                Schema: %s
                """.formatted(
                d.getName(),
                d.getDescription() != null ? d.getDescription() : "(no description)",
                d.getId(),
                d.getType(),
                d.getSchema()))
                .collect(Collectors.joining("\n---\n"));
    }

    @Tool(description = "Create a new data pipeline on Debezium Platform. "
            + "A pipeline connects a source to a destination to enable change data capture. "
            + "IMPORTANT: Always call first with userConfirmed=false to preview the configuration, "
            + "then ask the user for approval before calling again with userConfirmed=true to create.")
    @Transactional
    public String createPipeline(
                                 @ToolArg(description = "Set to false for preview, true to execute after user confirms") boolean userConfirmed,
                                 @Valid @ToolArg(description = "Pipeline configuration") AiPipeline aiPipeline) {

        if (!userConfirmed) {
            return String.format("""
                    ⚠️  CONFIRMATION REQUIRED ⚠️

                    You are about to CREATE A PIPELINE with the following details:
                    - Name: %s
                    - Description: %s
                    - Source: %s
                    - Destination: %s

                    This will create a new data pipeline on the Debezium Platform.

                    Please ask the user: "Do you want to proceed with creating this pipeline?"
                    If the user confirms, call this tool again with userConfirmed=true.
                    """,
                    aiPipeline.getName(),
                    aiPipeline.getDescription() != null ? aiPipeline.getDescription() : "(no description)",
                    aiPipeline.getSource() != null ? aiPipeline.getSource().getName() : "(not specified)",
                    aiPipeline.getDestination() != null ? aiPipeline.getDestination().getName() : "(not specified)");
        }

        try {
            Pipeline pipeline = evm.create(Pipeline.class);
            pipeline.setName(aiPipeline.getName());
            pipeline.setDescription(aiPipeline.getDescription());

            // Resolve source reference by finding source by name
            if (aiPipeline.getSource() != null) {
                String sourceName = aiPipeline.getSource().getName();
                Source source = sourceService.list().stream()
                        .filter(s -> s.getName().equals(sourceName))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Source not found: " + sourceName));
                SourceReference sourceRef = evm.getReference(SourceReference.class, source.getId());
                pipeline.setSource(sourceRef);
            }

            // Resolve destination reference by finding destination by name
            if (aiPipeline.getDestination() != null) {
                String destName = aiPipeline.getDestination().getName();
                Destination dest = destinationService.list().stream()
                        .filter(d -> d.getName().equals(destName))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Destination not found: " + destName));
                DestinationReference destRef = evm.getReference(DestinationReference.class, dest.getId());
                pipeline.setDestination(destRef);
            }

            if (aiPipeline.getLogLevel() != null) {
                pipeline.setLogLevel(aiPipeline.getLogLevel());
            }
            if (aiPipeline.getLogLevels() != null) {
                pipeline.setLogLevels(aiPipeline.getLogLevels());
            }

            Pipeline created = pipelineService.create(pipeline);
            return "Pipeline created successfully with ID: " + created.getId();

        }
        catch (Exception e) {
            return "Error creating pipeline: " + e.getMessage();
        }
    }

    @Tool(description = "Create a new data source connector on Debezium Platform. "
            + "A source captures change events from a database or data system. "
            + "Supported types include: postgresql, mysql, mongodb, sqlserver, oracle. "
            + "IMPORTANT: Always call first with userConfirmed=false to preview the configuration, "
            + "then ask the user for approval before calling again with userConfirmed=true to create.")
    @Transactional
    public String createSource(
                               @ToolArg(description = "Set to false for preview, true to execute after user confirms") boolean userConfirmed,
                               @Valid @ToolArg(description = "Source connector configuration") AiSource aiSource) {

        if (!userConfirmed) {
            return String.format("""
                    ⚠️  CONFIRMATION REQUIRED ⚠️

                    You are about to CREATE A SOURCE with the following details:
                    - Name: %s
                    - Type: %s
                    - Schema: %s
                    - Description: %s
                    - Config: %s

                    This will create a new data source connector on the Debezium Platform.

                    Please ask the user: "Do you want to proceed with creating this source?"
                    If the user confirms, call this tool again with userConfirmed=true.
                    """,
                    aiSource.getName(),
                    aiSource.getType(),
                    aiSource.getSchema(),
                    aiSource.getDescription() != null ? aiSource.getDescription() : "(no description)",
                    aiSource.getConfig() != null ? aiSource.getConfig().toString() : "(no additional config)");
        }

        try {
            Source source = evm.create(Source.class);
            source.setName(aiSource.getName());
            source.setType(aiSource.getType());
            source.setSchema(aiSource.getSchema());
            source.setDescription(aiSource.getDescription());

            if (aiSource.getConfig() != null) {
                source.setConfig(aiSource.getConfig());
            }

            if (aiSource.getVaults() != null && !aiSource.getVaults().isEmpty()) {
                source.setVaults(new HashSet<>(aiSource.getVaults().stream()
                        .map(v -> {
                            String vaultName = v.getName();
                            var vault = vaultService.list().stream()
                                    .filter(vlt -> vlt.getName().equals(vaultName))
                                    .findFirst()
                                    .orElseThrow(() -> new IllegalArgumentException("Vault not found: " + vaultName));
                            return evm.getReference(io.debezium.platform.domain.views.refs.VaultReference.class, vault.getId());
                        })
                        .toList()));
            }

            Source created = sourceService.create(source);
            return "Source created successfully with ID: " + created.getId();

        }
        catch (Exception e) {
            return "Error creating source: " + e.getMessage();
        }
    }

    @Tool(description = "Create a new destination connector on Debezium Platform. "
            + "A destination receives change events from sources via pipelines. "
            + "Supported types include: kafka, http, elasticsearch. "
            + "IMPORTANT: Always call first with userConfirmed=false to preview the configuration, "
            + "then ask the user for approval before calling again with userConfirmed=true to create.")
    @Transactional
    public String createDestination(
                                    @ToolArg(description = "Set to false for preview, true to execute after user confirms") boolean userConfirmed,
                                    @Valid @ToolArg(description = "Destination connector configuration") AiDestination aiDestination) {

        if (!userConfirmed) {
            return String.format("""
                    ⚠️  CONFIRMATION REQUIRED ⚠️

                    You are about to CREATE A DESTINATION with the following details:
                    - Name: %s
                    - Type: %s
                    - Schema: %s
                    - Description: %s
                    - Config: %s

                    This will create a new destination connector on the Debezium Platform.

                    Please ask the user: "Do you want to proceed with creating this destination?"
                    If the user confirms, call this tool again with userConfirmed=true.
                    """,
                    aiDestination.getName(),
                    aiDestination.getType(),
                    aiDestination.getSchema(),
                    aiDestination.getDescription() != null ? aiDestination.getDescription() : "(no description)",
                    aiDestination.getConfig() != null ? aiDestination.getConfig().toString() : "(no additional config)");
        }

        try {
            Destination destination = evm.create(Destination.class);
            destination.setName(aiDestination.getName());
            destination.setType(aiDestination.getType());
            destination.setSchema(aiDestination.getSchema());
            destination.setDescription(aiDestination.getDescription());

            if (aiDestination.getConfig() != null) {
                destination.setConfig(aiDestination.getConfig());
            }

            if (aiDestination.getVaults() != null && !aiDestination.getVaults().isEmpty()) {
                destination.setVaults(new HashSet<>(aiDestination.getVaults().stream()
                        .map(v -> {
                            String vaultName = v.getName();
                            var vault = vaultService.list().stream()
                                    .filter(vlt -> vlt.getName().equals(vaultName))
                                    .findFirst()
                                    .orElseThrow(() -> new IllegalArgumentException("Vault not found: " + vaultName));
                            return evm.getReference(io.debezium.platform.domain.views.refs.VaultReference.class, vault.getId());
                        })
                        .toList()));
            }

            Destination created = destinationService.create(destination);
            return "Destination created successfully with ID: " + created.getId();

        }
        catch (Exception e) {
            return "Error creating destination: " + e.getMessage();
        }
    }
}
