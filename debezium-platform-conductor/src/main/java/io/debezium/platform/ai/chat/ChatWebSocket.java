/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.ai.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.debezium.common.annotation.Incubating;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;

/**
 * WebSocket endpoint for chat using quarkus-websockets-next.
 * This API properly supports @SessionScoped beans for chat memory.
 *
 * @author Mario Fiore Vitale
 */
@Incubating
@WebSocket(path = "/api/chat")
public class ChatWebSocket {

    private static final Logger LOG = LoggerFactory.getLogger(ChatWebSocket.class);

    private final ChatService chatService;

    public ChatWebSocket(ChatService chatService) {
        this.chatService = chatService;
    }

    @OnOpen
    public void onOpen(WebSocketConnection connection) {
        LOG.info("WebSocket opened: {}", connection.id());
        connection.sendTextAndAwait("{\"type\":\"connected\",\"message\":\"Connected to chat service\"}");
    }

    @OnClose
    public void onClose(WebSocketConnection connection) {
        LOG.info("WebSocket closed: {}", connection.id());
    }

    @OnError
    public void onError(WebSocketConnection connection, Throwable throwable) {
        LOG.error("WebSocket error on connection: {}", connection.id(), throwable);
        connection.sendTextAndAwait("{\"type\":\"error\",\"message\":\"" + escapeJson(throwable.getMessage()) + "\"}");
    }

    @OnTextMessage
    public void onMessage(String message, WebSocketConnection connection) {
        LOG.info("Received message from {}: {}", connection.id(), message);

        try {
            // Send processing indicator
            connection.sendTextAndAwait("{\"type\":\"processing\",\"message\":\"Processing your request...\"}");

            chatService.chatWithLogging(message).whenComplete((response, error) -> {
                if (error != null) {
                    LOG.error("Error processing chat message", error);
                    connection.sendTextAndAwait("{\"type\":\"error\",\"message\":\"" + escapeJson(error.getMessage()) + "\"}");
                }
                else {
                    // Send the complete final answer
                    String jsonResponse = "{\"type\":\"answer\",\"content\":\"" + escapeJson(response) + "\"}";
                    connection.sendTextAndAwait(jsonResponse);
                }
            });
        }
        catch (Exception e) {
            LOG.error("Error handling message", e);
            connection.sendTextAndAwait("{\"type\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
