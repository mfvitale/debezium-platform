/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.environment.operator.actions;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import io.debezium.DebeziumException;
import io.debezium.operator.api.model.DebeziumServerBuilder;
import io.debezium.platform.data.dto.SignalRequest;
import io.debezium.platform.environment.actions.client.DebeziumServerClient;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;

@EnableKubernetesMockClient(crud = true)
class DebeziumServerProxyTest {

    private KubernetesClient kubernetesClient;
    private DebeziumServerProxy proxy;

    @Mock
    private DebeziumServerClient debeziumServerClient;

    @BeforeEach
    void setUp() {

        initMocks(this);

        proxy = new DebeziumServerProxy(kubernetesClient, debeziumServerClient);
    }

    @Test
    @DisplayName("Correctly send signal to the correct service")
    void sendSignal() {

        createServices();
        var signal = new SignalRequest("1", "execute-snapshot", "{ \"data-collections\": [ \"inventory.products\"],\"type\": \"INCREMENTAL\"}", Map.of());
        when(debeziumServerClient.sendSignal("http://test-pipeline-api:8080", signal)).thenReturn(Response.accepted().build());

        var dsSpec = new DebeziumServerBuilder().withMetadata(new ObjectMetaBuilder()
                .withNamespace("my-namespace")
                .withName("test-pipeline")
                .build())
                .build();

        proxy.sendSignal(signal, dsSpec);

        verify(debeziumServerClient, times(1)).sendSignal("http://test-pipeline-api:8080", signal);
    }

    @Test
    @DisplayName("Signal is not sent when no pipeline associated service is found")
    void noService() {

        var signal = new SignalRequest("1", "execute-snapshot", "{ \"data-collections\": [ \"inventory.products\"],\"type\": \"INCREMENTAL\"}", Map.of());
        var dsSpec = new DebeziumServerBuilder().withMetadata(new ObjectMetaBuilder()
                .withNamespace("my-namespace")
                .withName("test-pipeline")
                .build())
                .build();

        assertThatExceptionOfType(DebeziumException.class)
                .isThrownBy(() -> proxy.sendSignal(signal, dsSpec))
                .withMessage("Unable to find pipeline instance to send the signal");
    }

    @Test
    @DisplayName("An error is throw when error occurs during api call")
    void errorOnApiCall() {

        createServices();
        var signal = new SignalRequest("1", "execute-snapshot", "{ \"data-collections\": [ \"inventory.products\"],\"type\": \"INCREMENTAL\"}", Map.of());
        when(debeziumServerClient.sendSignal("http://test-pipeline-api:8080", signal)).thenReturn(Response.serverError().build());

        var dsSpec = new DebeziumServerBuilder().withMetadata(new ObjectMetaBuilder()
                .withNamespace("my-namespace")
                .withName("test-pipeline")
                .build())
                .build();

        assertThatExceptionOfType(DebeziumException.class)
                .isThrownBy(() -> proxy.sendSignal(signal, dsSpec))
                .withCauseInstanceOf(DebeziumException.class)
                .havingCause()
                .withMessage("Unable to to send signal to http://test-pipeline-api:8080 for Internal Server Error");
    }

    private void createServices() {
        kubernetesClient.services()
                .resource(createNewService("test-pipeline-api",
                        Map.of("debezium.io/classifier", "api",
                                "debezium.io/instance", "test-pipeline"),
                        "my-namespace"))
                .create();

        kubernetesClient.services()
                .resource(createNewService("test-pipeline2-api",
                        Map.of("debezium.io/classifier", "api",
                                "debezium.io/instance", "test-pipeline2"),
                        "another-ns"))
                .create();
    }

    private Service createNewService(String name, Map<String, String> labels, String namespace) {
        return new ServiceBuilder()
                .withNewMetadata()
                .withNamespace(namespace)
                .withName(name)
                .addToLabels(labels)
                .endMetadata()
                .withNewSpec()
                .withPorts(new ServicePort("TCP", "http", 8080, 8080, "TCP", new IntOrString(8080)))
                .endSpec()
                .build();
    }

}
