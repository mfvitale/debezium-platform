/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.agent;

/**
 * Request body for the {@code POST /api/agent/deploy} endpoint.
 *
 * <p>Contains everything the Agent needs to start a Debezium Server container:
 * the container name, Docker image, host port to bind, and the full
 * {@code application.properties} content that will be written to disk
 * and bind-mounted into the container.
 *
 * @param containerName  Docker container name (e.g. {@code debezium-pipeline-42})
 * @param image          Docker image to run (e.g. {@code quay.io/debezium/server:latest})
 * @param port           host port to map to the container's 8080
 * @param configContent  full {@code application.properties} content for Debezium Server
 */
public record DeployRequest(
        String containerName,
        String image,
        int port,
        String configContent) {
}
