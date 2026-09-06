/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.environment.host.agent;

/**
 * Request body for Agent endpoints that only need a container name.
 *
 * @param containerName  the Docker container name
 */
public record AgentContainerNameRequest(String containerName) {
}
