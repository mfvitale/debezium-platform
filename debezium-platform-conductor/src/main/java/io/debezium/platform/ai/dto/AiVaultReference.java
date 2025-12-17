/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.ai.dto;

/**
 * AI-specific vault reference DTO for MCP tools.
 * Simplified version for LLM interaction.
 */
public class AiVaultReference {
    private String name;
    private Long id;

    public AiVaultReference() {
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
