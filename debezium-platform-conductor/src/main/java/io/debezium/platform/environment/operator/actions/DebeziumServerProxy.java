/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.environment.operator.actions;

import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.debezium.DebeziumException;
import io.debezium.operator.api.model.DebeziumServer;
import io.debezium.platform.data.dto.SignalRequest;
import io.debezium.platform.environment.actions.client.DebeziumServerClient;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.KubernetesClient;

@ApplicationScoped
public class DebeziumServerProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(DebeziumServerProxy.class);

    private static final String DEBEZIUM_IO_CLASSIFIER_LABEL = "debezium.io/classifier";
    private static final String DEBEZIUM_IO_INSTANCE_LABEL = "debezium.io/instance";
    private static final String API_CLASSIFIER = "api";
    private static final String SERVICE_URL_FORMAT = "http://%s:%s";

    private final KubernetesClient k8s;
    private final DebeziumServerClient dsClient;

    public DebeziumServerProxy(KubernetesClient k8s, @RestClient DebeziumServerClient dsClient) {
        this.k8s = k8s;
        this.dsClient = dsClient;
    }

    public void sendSignal(SignalRequest signalRequest, DebeziumServer ds) {

        var baseUrl = getDSApiBaseUrl(ds);
        if (baseUrl.isEmpty()) {
            // TODO should we return a status response at upper level?
            return;
        }

        try {

            try (Response response = dsClient.sendSignal(baseUrl.get(), signalRequest)) {

                LOGGER.debug("Call to {} returned with {}", baseUrl, response);
                // TODO should we return a status response at upper level?
            }
        }
        catch (Exception e) {
            throw new DebeziumException(String.format("Error sending signal to %s ", baseUrl), e);
        }
    }

    private Optional<String> getDSApiBaseUrl(DebeziumServer ds) {

        var namespace = ds.getMetadata().getNamespace();
        var requiredLabels = Map.of(
                DEBEZIUM_IO_CLASSIFIER_LABEL, API_CLASSIFIER,
                DEBEZIUM_IO_INSTANCE_LABEL, ds.getMetadata().getName());

        ServiceList apiServices = k8s.services()
                .inNamespace(namespace)
                .withLabels(requiredLabels)
                .list();

        if (apiServices.getItems().isEmpty()) {
            LOGGER.debug("No service found in the ns {} with labels {}", namespace, requiredLabels);
            return Optional.empty();
        }

        Service apiService = apiServices.getItems().getFirst();

        if (apiService.getSpec().getPorts().isEmpty()) {
            LOGGER.debug("Found service {} in the ns {} without any ports", apiService.getMetadata().getName(), namespace);
            return Optional.empty();
        }

        var port = apiService.getSpec().getPorts().getFirst().getPort();

        return Optional.of(String.format(SERVICE_URL_FORMAT, apiService.getMetadata().getName(), port));
    }
}
