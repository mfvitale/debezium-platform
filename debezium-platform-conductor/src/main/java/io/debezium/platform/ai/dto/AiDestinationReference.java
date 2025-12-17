/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.ai.dto;

import jakarta.validation.constraints.NotEmpty;

/**
 * AI-specific destination reference DTO for MCP tools.
 * References an existing destination by name for pipeline creation.
 */
public class AiDestinationReference {
    @NotEmpty
    private String name;

    private Long id;

    public AiDestinationReference() {
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
