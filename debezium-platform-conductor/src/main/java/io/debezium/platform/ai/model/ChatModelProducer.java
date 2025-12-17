/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.ai.model;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.debezium.common.annotation.Incubating;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.profile.IfBuildProfile;

import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

/**
 * Produces profile-based StreamingChatModel beans.
 * - Dev profile: CustomVertexAiAnthropicStreamingChatModel with SSL trust-all
 * - Production: Standard AnthropicStreamingChatModel
 */
@Incubating
@ApplicationScoped
public class ChatModelProducer {

    @ConfigProperty(name = "quarkus.langchain4j.anthropic.api-key")
    String apiKey;

    @ConfigProperty(name = "quarkus.langchain4j.anthropic.base-url")
    String baseUrl;

    @ConfigProperty(name = "quarkus.langchain4j.anthropic.chat-model.model-name")
    String modelName;

    @ConfigProperty(name = "quarkus.langchain4j.anthropic.chat-model.log-requests", defaultValue = "false")
    boolean logRequests;

    @ConfigProperty(name = "quarkus.langchain4j.anthropic.chat-model.log-responses", defaultValue = "false")
    boolean logResponses;

    /**
     * Production chat model - standard Anthropic client
     */
    @Produces
    @DefaultBean
    @ApplicationScoped
    public StreamingChatModel productionChatModel() {
        return AnthropicStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();
    }

    /**
     * Dev mode chat model - custom implementation with SSL trust-all
     */
    @Produces
    @IfBuildProfile("dev")
    @ApplicationScoped
    public StreamingChatModel devChatModel() {
        return CustomVertexAiAnthropicStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .systemPrompt(getSystemPrompt())
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();
    }

    private String getSystemPrompt() {
        return """
                FORMATTING RULES - STRICTLY FOLLOW THESE:
                - Use ONLY plain text - NO markdown syntax at all
                - Do NOT use **bold**, _italic_, `code`, or any markdown formatting
                - Do NOT use # headers or --- dividers
                - Use UPPERCASE for emphasis on important names and terms
                - Use simple indentation and line breaks for structure
                - Use simple bullet points with - or • characters only
                - Keep all formatting minimal and readable as plain text

                Example of CORRECT formatting:
                CONFIGURATION PREVIEW:
                - Name: postgres-source
                - Type: PostgreSQL

                Example of WRONG formatting (DO NOT DO THIS):
                **Configuration Preview:**
                - **Name:** postgres-source

                CRITICAL: Do NOT show tool invocation syntax (like <invoke>, <parameter>, etc.) in your responses.
                Tool calls happen automatically in the background - only show the results to the user.
                Present information naturally as if you performed the action yourself.

                IMPORTANT SAFETY RULE: For any WRITE operations (createSource, createDestination, createPipeline),
                you MUST ALWAYS call the tool first with userConfirmed=false to get the confirmation details,
                then present those details to the user and ask for their explicit confirmation,
                and ONLY after the user confirms should you call the tool again with userConfirmed=true.
                NEVER execute write operations without user confirmation.
                """;
    }
}
