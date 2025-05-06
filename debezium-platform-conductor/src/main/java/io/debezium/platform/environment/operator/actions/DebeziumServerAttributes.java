/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.environment.operator.actions;

/**
 * Record that encapsulates identifying attributes of a Debezium Server instance.
 * <p>
 * This record holds the namespace and name of a Debezium Server resource in a Kubernetes environment.
 * It serves as a convenient data carrier for Debezium Server identification information
 * throughout the application.
 * </p>
 *
 * @param namespace The Kubernetes namespace where the Debezium Server instance is deployed.
 * @param name The name of the Debezium Server instance.
 */
public record DebeziumServerAttributes(String namespace, String name) {
}
