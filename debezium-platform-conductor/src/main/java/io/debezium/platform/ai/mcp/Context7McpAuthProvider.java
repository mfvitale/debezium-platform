/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.ai.mcp;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkiverse.langchain4j.mcp.auth.McpClientAuthProvider;

/**
 * Authorization provider for Context7 MCP client.
 * Injects the CONTEXT7_API_KEY as a Bearer token in the Authorization header.
 */
@ApplicationScoped
public class Context7McpAuthProvider implements McpClientAuthProvider {

    public static final String CONTEXT_7_DOMAIN = "context7.com";
    @ConfigProperty(name = "context7.api-key", defaultValue = "")
    String apiKey;

    @Override
    public String getAuthorization(Input input) {
        // Only provide auth for Context7 MCP server
        if (input.uri().toString().contains(CONTEXT_7_DOMAIN)) {
            if (apiKey != null && !apiKey.isEmpty()) {
                return "Bearer " + apiKey;
            }
        }
        return null;
    }
}
