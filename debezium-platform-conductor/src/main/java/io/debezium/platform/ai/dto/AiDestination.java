/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.ai.dto;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotEmpty;

/**
 * AI-specific destination DTO for MCP tools.
 * Simplified version optimized for LLM interaction with validation for schema generation.
 */
public class AiDestination {
    private String description;

    @NotEmpty
    private String type;

    @NotEmpty
    private String schema;

    private List<AiVaultReference> vaults;
    private Map<String, Object> config;

    @NotEmpty
    private String name;

    private Long id;

    public AiDestination() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public List<AiVaultReference> getVaults() {
        return vaults;
    }

    public void setVaults(List<AiVaultReference> vaults) {
        this.vaults = vaults;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
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
}
