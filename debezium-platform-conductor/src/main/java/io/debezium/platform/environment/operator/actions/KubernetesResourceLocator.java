/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.environment.operator.actions;

import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.KubernetesClient;

@ApplicationScoped
public class KubernetesResourceLocator {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesResourceLocator.class);

    private static final String DEBEZIUM_IO_CLASSIFIER_LABEL = "debezium.io/classifier";
    private static final String DEBEZIUM_IO_INSTANCE_LABEL = "debezium.io/instance";
    private static final String API_CLASSIFIER = "api";
    private static final String SERVICE_URL_FORMAT = "http://%s:%s";

    private final KubernetesClient kubernetesClient;

    public KubernetesResourceLocator(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    Optional<String> getApiBaseUrl(DebeziumServerAttributes debeziumServerAttributes) {

        var requiredLabels = Map.of(
                DEBEZIUM_IO_CLASSIFIER_LABEL, API_CLASSIFIER,
                DEBEZIUM_IO_INSTANCE_LABEL, debeziumServerAttributes.name());

        ServiceList apiServices = kubernetesClient.services()
                .inNamespace(debeziumServerAttributes.namespace())
                .withLabels(requiredLabels)
                .list();

        if (apiServices.getItems().isEmpty()) {
            LOGGER.error("No service found in the ns {} with labels {}", debeziumServerAttributes.namespace(), requiredLabels);
            return Optional.empty();
        }

        Service apiService = apiServices.getItems().getFirst();

        if (apiService.getSpec().getPorts().isEmpty()) {
            LOGGER.error("Found service {} in the ns {} without any ports", apiService.getMetadata().getName(), debeziumServerAttributes.namespace());
            return Optional.empty();
        }

        var port = apiService.getSpec().getPorts().getFirst().getPort();

        return Optional.of(String.format(SERVICE_URL_FORMAT, apiService.getMetadata().getName(), port));
    }
}
