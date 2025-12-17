/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.ai.chat;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import jakarta.enterprise.context.SessionScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.debezium.common.annotation.Incubating;

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;

/**
 * ChatService using fully declarative Quarkus LangChain4j approach.
 * - Assistant is auto-generated via @RegisterAiService
 * - MCP tools configured declaratively in application.properties
 * - StreamingChatModel injected from ChatModelProducer (profile-based)
 *
 * @author Mario Fiore Vitale
 */
@Incubating
@SessionScoped
public class ChatService implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(ChatService.class);

    private final Zium zium;

    public ChatService(Zium zium) {
        this.zium = zium;
    }

    /**
     * Chat method for WebSocket - logs intermediate responses, returns only final answer
     */
    public CompletableFuture<String> chatWithLogging(String message) {
        CompletableFuture<String> futureResponse = new CompletableFuture<>();

        LOG.info("Processing chat message: {}", message);

        TokenStream tokenStream = zium.chat(message);

        tokenStream
                .onPartialResponse((String partialResponse) -> {
                    LOG.debug("Partial response: {}", partialResponse);
                })
                .onRetrieved((List<Content> contents) -> {
                    LOG.info("Retrieved contents: {}", contents);
                })
                .onIntermediateResponse((ChatResponse intermediateResponse) -> {
                    LOG.info("Intermediate response: {}", intermediateResponse);
                })
                .onToolExecuted((ToolExecution toolExecution) -> {
                    LOG.info("Tool execution result: {}", toolExecution);
                })
                .onCompleteResponse((ChatResponse response) -> {
                    LOG.info("Chat completed successfully");
                    String finalAnswer = response.aiMessage().text();
                    futureResponse.complete(finalAnswer);
                })
                .onError((Throwable error) -> {
                    LOG.error("Error during chat processing", error);
                    futureResponse.completeExceptionally(error);
                })
                .start();

        return futureResponse;
    }
}
