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

/**
 * A proxy class for interacting with Debezium Server instances associate to a pipeline in a Kubernetes environment.
 * <p>
 * This class serves as a bridge between the conductor and Debezium Server deployments,
 * facilitating the sending of signals to control Debezium Server behavior. It uses the
 * {@link DebeziumServerClient} to communicate with the Debezium Server REST API and
 * {@link DebeziumKubernetesAdapter} to find the appropriate Debezium Server instances
 * in the Kubernetes cluster.
 * </p>
 */
@ApplicationScoped
public class DebeziumServerProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(DebeziumServerProxy.class);

    private final DebeziumServerClient dsClient;
    private final DebeziumKubernetesAdapter kubernetesResourceLocator;

    /**
     * Constructs a new DebeziumServerProxy with the specified dependencies.
     *
     * @param dsClient The REST client for communicating with Debezium Server instances.
     * @param debeziumKubernetesAdapter The service for locating Kubernetes resources within the cluster.
     */
    public DebeziumServerProxy(@RestClient DebeziumServerClient dsClient, DebeziumKubernetesAdapter debeziumKubernetesAdapter) {
        this.dsClient = dsClient;
        this.kubernetesResourceLocator = debeziumKubernetesAdapter;
    }

    /**
     * Sends a signal to the specified Debezium Server instance.
     * <p>
     * This method locates the API endpoint for the given Debezium Server using the
     * KubernetesResourceLocator and then sends the provided signal to that endpoint.
     * </p>
     *
     * @param signal The signal to send to the Debezium Server instance.
     * @param debeziumServer The Debezium Server instance used by the {@link DebeziumKubernetesAdapter} to
     *                       get the related API service base URL.
     * @throws DebeziumException If the Debezium Server instance cannot be found or if the signal
     *                           transmission fails.
     */
    public void sendSignal(Signal signal, DebeziumServer debeziumServer) {

        DebeziumServerAttributes debeziumServerAttributes = new DebeziumServerAttributes(
                debeziumServer.getMetadata().getNamespace(),
                debeziumServer.getMetadata().getName());

        kubernetesResourceLocator.getServiceApiBaseUrl(debeziumServerAttributes)
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
