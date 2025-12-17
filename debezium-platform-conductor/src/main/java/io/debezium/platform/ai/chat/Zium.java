/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.ai.chat;

import jakarta.enterprise.context.SessionScoped;

import io.debezium.common.annotation.Incubating;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.mcp.runtime.McpToolBox;

import dev.langchain4j.service.TokenStream;

/**
 * Debezium Platform AI Assistant using declarative Quarkus LangChain4j.
 * Uses MCP protocol to access Platform tools.
 */
@Incubating
@RegisterAiService
@SessionScoped
public interface Zium {

    @McpToolBox({ "debezium-platform", "context7" })
    TokenStream chat(String userMessage);
}
