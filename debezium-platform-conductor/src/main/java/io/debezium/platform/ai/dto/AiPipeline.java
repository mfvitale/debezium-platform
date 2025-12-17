/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.ai.dto;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * AI-specific pipeline DTO for MCP tools.
 * Simplified version optimized for LLM interaction with validation for schema generation.
 */
public class AiPipeline {
    @NotEmpty
    private String name;

    private Long id;
    private String description;

    @Valid
    @NotNull
    private AiSourceReference source;

    @Valid
    @NotNull
    private AiDestinationReference destination;

    private List<AiTransformReference> transforms;
    private String logLevel;
    private Map<String, String> logLevels;

    public AiPipeline() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AiSourceReference getSource() {
        return source;
    }

    public void setSource(AiSourceReference source) {
        this.source = source;
    }

    public AiDestinationReference getDestination() {
        return destination;
    }

    public void setDestination(AiDestinationReference destination) {
        this.destination = destination;
    }

    public List<AiTransformReference> getTransforms() {
        return transforms;
    }

    public void setTransforms(List<AiTransformReference> transforms) {
        this.transforms = transforms;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public Map<String, String> getLogLevels() {
        return logLevels;
    }

    public void setLogLevels(Map<String, String> logLevels) {
        this.logLevels = logLevels;
    }
}
