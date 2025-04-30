/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.environment.operator.actions;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.debezium.DebeziumException;
import io.debezium.operator.api.model.DebeziumServer;
import io.debezium.platform.domain.Signal;
import io.debezium.platform.environment.actions.client.DebeziumServerClient;

@ApplicationScoped
public class DebeziumServerProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(DebeziumServerProxy.class);

    private final DebeziumServerClient dsClient;
    private final KubernetesResourceLocator kubernetesResourceLocator;

    public DebeziumServerProxy(@RestClient DebeziumServerClient dsClient, KubernetesResourceLocator kubernetesResourceLocator) {
        this.dsClient = dsClient;
        this.kubernetesResourceLocator = kubernetesResourceLocator;
    }

    public void sendSignal(Signal signal, DebeziumServer debeziumServer) {

        DebeziumServerAttributes debeziumServerAttributes = new DebeziumServerAttributes(
                debeziumServer.getMetadata().getNamespace(),
                debeziumServer.getMetadata().getName());

        kubernetesResourceLocator.getApiBaseUrl(debeziumServerAttributes)
                .ifPresentOrElse(baseUrl -> send(baseUrl, signal),
                        () -> {
                            throw new DebeziumException("Unable to find pipeline instance to send the signal");
                        });
    }

    private void send(String dsApiBaseUrl, Signal signal) {

        try (Response response = dsClient.sendSignal(dsApiBaseUrl, signal)) {

            LOGGER.debug("Call to {} returned with {}", dsApiBaseUrl, response.getStatusInfo().getReasonPhrase());
            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                LOGGER.error("Sending signal to {} failed with {}", dsApiBaseUrl, response.getStatusInfo().getReasonPhrase());
                throw new DebeziumException(String.format("Unable to to send signal to %s for %s", dsApiBaseUrl, response.getStatusInfo().getReasonPhrase()));
            }
        }
        catch (RuntimeException e) {
            throw new DebeziumException(String.format("Error sending signal to %s ", dsApiBaseUrl), e);
        }
    }

}
