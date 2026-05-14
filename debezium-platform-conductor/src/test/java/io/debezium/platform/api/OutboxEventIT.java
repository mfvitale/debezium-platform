/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.agroal.api.AgroalDataSource;
import io.debezium.doc.FixFor;
import io.debezium.platform.OutboxTestProfile;
import io.debezium.platform.environment.operator.actions.DebeziumKubernetesAdapter;
import io.debezium.platform.util.TestDatasourceHelper;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(OutboxTestProfile.class)
@EnableKubernetesMockClient(crud = true)
class OutboxEventIT {

    @Inject
    InjectableInstance<AgroalDataSource> dataSource;

    @InjectMock
    DebeziumKubernetesAdapter k8sAdapter;

    @ConfigProperty(name = "quarkus.datasource.jdbc.url")
    String datasourceUrl;

    Long sourceConnectionId;
    Long sourceId;
    Long destinationId;
    Long transformId;
    Long pipelineId;
    String resourceSuffix;

    @BeforeEach
    void setUp() {
        resourceSuffix = String.valueOf(System.nanoTime());

        TestDatasourceHelper dbHelper = TestDatasourceHelper.parsePostgresJdbcUrl(datasourceUrl);

        sourceConnectionId = createResource("api/connections", """
                {
                       "name": "postgres-connection-%s",
                       "type": "POSTGRESQL",
                       "config": {
                         "hostname": "postgresql",
                         "port": %s,
                         "username": "debezium",
                         "password": "debezium",
                         "database": "debezium"
                       }
                     }
                  }""".formatted(resourceSuffix, dbHelper.getPort()));

        Long destinationConnectionId = createResource("api/connections", """
                {
                     "name": "kafka-connection-%s",
                     "type": "KAFKA",
                     "config": {
                       "bootstrap.servers": "dbz-kafka-kafka-bootstrap.debezium-platform:9092"
                     }
                   }""".formatted(resourceSuffix));

        sourceId = createResource("api/sources", """
                {
                    "name": "test-source-%s",
                    "description": "Test source for outbox",
                    "type": "io.debezium.connector.postgresql.PostgresConnector",
                    "schema": "dummy",
                    "vaults": [],
                    "connection": {
                        "id": %s
                    },
                    "config": {
                      "topic.prefix": "inventory",
                      "schema.include.list": "inventory"
                    }
                  }""".formatted(resourceSuffix, sourceConnectionId));

        destinationId = createResource("api/destinations", """
                 {
                  "name": "test-destination-%s",
                  "type": "kafka",
                  "description": "Test destination for outbox",
                  "schema": "dummy",
                  "vaults": [],
                   "connection": {
                        "id": %s
                    },
                  "config": {
                    "producer.key.serializer": "org.apache.kafka.common.serialization.StringSerializer",
                    "producer.value.serializer": "org.apache.kafka.common.serialization.StringSerializer"
                  }
                }""".formatted(resourceSuffix, destinationConnectionId));

        transformId = createResource("api/transforms", """
                {
                   "name": "test-transform-%s",
                   "description": "Extract Debezium payload",
                   "type": "io.debezium.transforms.ExtractNewRecordState",
                   "schema": "string",
                   "vaults": [],
                   "config": {
                     "add.fields": "op"
                   }
                 }""".formatted(resourceSuffix));

        pipelineId = createResource("api/pipelines", """
                {
                   "name": "test-pipeline-%s",
                   "description": "Pipeline for outbox test",
                   "source": {
                     "id": %s,
                     "name": "test-source-%s"
                   },
                   "destination": {
                     "id": %s,
                     "name": "test-destination-%s"
                   },
                   "transforms": [
                     {
                        "id": %s,
                        "name": "test-transform-%s"
                     }
                   ],
                   "logLevel": "INFO",
                   "logLevels": {}
                 }""".formatted(resourceSuffix, sourceId, resourceSuffix, destinationId, resourceSuffix, transformId, resourceSuffix));

        clearEventsTable();
    }

    @Test
    @DisplayName("When a pipeline is updated directly then an outbox event must be created")
    void updatingPipelineShouldCreateOutboxEvent() {

        given()
                .header("Content-Type", "application/json")
                .body("""
                        {
                           "name": "test-pipeline-%s",
                           "description": "Pipeline for outbox test - updated",
                           "source": {
                             "id": %s,
                             "name": "test-source-%s"
                           },
                           "destination": {
                             "id": %s,
                             "name": "test-destination-%s"
                           },
                           "transforms": [
                             {
                                "id": %s,
                                "name": "test-transform-%s"
                             }
                           ],
                           "logLevel": "DEBUG",
                           "logLevels": {}
                         }""".formatted(resourceSuffix, sourceId, resourceSuffix, destinationId, resourceSuffix, transformId, resourceSuffix))
                .when().put("api/pipelines/" + pipelineId)
                .then()
                .statusCode(200);

        List<OutboxEvent> events = findPipelineEvents(pipelineId);

        assertThat(events)
                .as("Updating a pipeline directly should produce an outbox event")
                .hasSize(1);

        assertThat(events.getFirst().type()).isEqualTo("UPDATE");
        assertThat(events.getFirst().aggregateType()).isEqualTo("pipeline");
    }

    @FixFor("debezium/dbz#1939")
    @Test
    @DisplayName("When a source referenced by a pipeline is updated then an outbox event for the pipeline must be created")
    void updatingSourceReferencedByPipelineShouldCreateOutboxEvent() {

        given()
                .header("Content-Type", "application/json")
                .body("""
                        {
                            "name": "test-source-%s",
                            "description": "Updated source description",
                            "type": "io.debezium.connector.postgresql.PostgresConnector",
                            "schema": "dummy",
                            "vaults": [],
                            "config": {
                              "topic.prefix": "inventory",
                              "schema.include.list": "inventory,public"
                            }
                          }""".formatted(resourceSuffix))
                .when().put("api/sources/" + sourceId)
                .then()
                .statusCode(200);

        List<OutboxEvent> events = findPipelineEvents(pipelineId);

        assertThat(events)
                .as("Updating a source referenced by a pipeline should produce an outbox event for the pipeline")
                .isNotEmpty();

        assertThat(events.getFirst().type()).isEqualTo("UPDATE");
        assertThat(events.getFirst().aggregateType()).isEqualTo("pipeline");
    }

    @FixFor("debezium/dbz#1939")
    @Test
    @DisplayName("When a connection used by a source referenced by a pipeline is updated then an outbox event for the pipeline must be created")
    void updatingConnectionReferencedBySourceOfPipelineShouldCreateOutboxEvent() {

        TestDatasourceHelper dbHelper = TestDatasourceHelper.parsePostgresJdbcUrl(datasourceUrl);

        given()
                .header("Content-Type", "application/json")
                .body("""
                        {
                               "name": "postgres-connection-%s",
                               "type": "POSTGRESQL",
                               "config": {
                                 "hostname": "new-postgresql-host",
                                 "port": %s,
                                 "username": "debezium",
                                 "password": "debezium",
                                 "database": "debezium"
                               }
                             }
                          }""".formatted(resourceSuffix, dbHelper.getPort()))
                .when().put("api/connections/" + sourceConnectionId)
                .then()
                .statusCode(200);

        List<OutboxEvent> events = findPipelineEvents(pipelineId);

        assertThat(events)
                .as("Updating a connection used by a source referenced by a pipeline should produce an outbox event for the pipeline")
                .isNotEmpty();

        assertThat(events.getFirst().type()).isEqualTo("UPDATE");
        assertThat(events.getFirst().aggregateType()).isEqualTo("pipeline");
    }

    @FixFor("debezium/dbz#1939")
    @Test
    @DisplayName("When a transform referenced by a pipeline is updated then an outbox event for the pipeline must be created")
    void updatingTransformReferencedByPipelineShouldCreateOutboxEvent() {

        given()
                .header("Content-Type", "application/json")
                .body("""
                        {
                           "name": "test-transform-%s",
                           "description": "Updated transform description",
                           "type": "io.debezium.transforms.ExtractNewRecordState",
                           "schema": "string",
                           "vaults": [],
                           "config": {
                             "add.fields": "op,table"
                           }
                         }""".formatted(resourceSuffix))
                .when().put("api/transforms/" + transformId)
                .then()
                .statusCode(200);

        List<OutboxEvent> events = findPipelineEvents(pipelineId);

        assertThat(events)
                .as("Updating a transform referenced by a pipeline should produce an outbox event for the pipeline")
                .isNotEmpty();

        assertThat(events.getFirst().type()).isEqualTo("UPDATE");
        assertThat(events.getFirst().aggregateType()).isEqualTo("pipeline");
    }

    private static Long createResource(String path, String body) {
        Number id = given()
                .header("Content-Type", "application/json")
                .body(body).when().post(path)
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        return id.longValue();
    }

    private void clearEventsTable() {
        try (Connection conn = dataSource.get().getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM events");
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<OutboxEvent> findPipelineEvents(Long pipelineId) {
        List<OutboxEvent> events = new ArrayList<>();
        try (Connection conn = dataSource.get().getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT aggregatetype, aggregateid, type FROM events WHERE aggregatetype = 'pipeline' AND aggregateid = ?")) {
            stmt.setString(1, pipelineId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    events.add(new OutboxEvent(
                            rs.getString("aggregatetype"),
                            rs.getString("aggregateid"),
                            rs.getString("type")));
                }
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return events;
    }

    record OutboxEvent(String aggregateType, String aggregateId, String type) {
    }
}
