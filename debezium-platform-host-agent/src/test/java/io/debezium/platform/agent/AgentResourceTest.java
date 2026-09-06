/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.agent;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/**
 * Tests for {@link AgentResource} endpoints.
 *
 * <p>Mocks {@link DockerCommandRunner} so tests run without a real
 * Docker daemon — only verifying HTTP contracts and request routing.
 */
@QuarkusTest
class AgentResourceTest {

    @InjectMock
    DockerCommandRunner docker;

    // ── Deploy ──

    @Test
    void deployReturns202Accepted() {
        org.mockito.Mockito.when(docker.run(org.mockito.ArgumentMatchers.any(String[].class)))
                .thenReturn(new DockerCommandRunner.CommandOutput(0, ""));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "containerName": "test-pipeline",
                          "image": "quay.io/debezium/server:latest",
                          "port": 9000,
                          "configContent": "debezium.sink.type=kafka"
                        }
                        """)
                .when()
                .post("/api/agent/deploy")
                .then()
                .statusCode(202);
    }

    // ── Undeploy ──

    @Test
    void undeployReturns200() {
        org.mockito.Mockito.when(docker.run(org.mockito.ArgumentMatchers.any(String[].class)))
                .thenReturn(new DockerCommandRunner.CommandOutput(0, ""));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "containerName": "test-pipeline" }
                        """)
                .when()
                .post("/api/agent/undeploy")
                .then()
                .statusCode(200);
    }

    // ── Stop ──

    @Test
    void stopReturns200OnSuccess() {
        org.mockito.Mockito.when(docker.run(org.mockito.ArgumentMatchers.any(String[].class)))
                .thenReturn(new DockerCommandRunner.CommandOutput(0, "test-pipeline"));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "containerName": "test-pipeline" }
                        """)
                .when()
                .post("/api/agent/stop")
                .then()
                .statusCode(200);
    }

    @Test
    void stopReturns500OnFailure() {
        org.mockito.Mockito.when(docker.run(org.mockito.ArgumentMatchers.any(String[].class)))
                .thenReturn(new DockerCommandRunner.CommandOutput(1, "No such container"));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "containerName": "nonexistent" }
                        """)
                .when()
                .post("/api/agent/stop")
                .then()
                .statusCode(500);
    }

    // ── Start ──

    @Test
    void startReturns200OnSuccess() {
        org.mockito.Mockito.when(docker.run(org.mockito.ArgumentMatchers.any(String[].class)))
                .thenReturn(new DockerCommandRunner.CommandOutput(0, "test-pipeline"));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "containerName": "test-pipeline" }
                        """)
                .when()
                .post("/api/agent/start")
                .then()
                .statusCode(200);
    }

    // ── Status ──

    @Test
    void statusReturns200WithRunningContainer() throws IOException {
        // Mock docker inspect returning "true"
        org.mockito.Mockito.when(docker.run(
                org.mockito.ArgumentMatchers.eq("docker"),
                org.mockito.ArgumentMatchers.eq("inspect"),
                org.mockito.ArgumentMatchers.eq("--format"),
                org.mockito.ArgumentMatchers.eq("{{.State.Running}}"),
                org.mockito.ArgumentMatchers.eq("test-pipeline")))
                .thenReturn(new DockerCommandRunner.CommandOutput(0, "true"));

        // Create a config file so the hash can be computed
        Path configDir = Path.of(System.getProperty("java.io.tmpdir"), "test-configs", "test-pipeline");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("application.properties");
        Files.writeString(configFile, "debezium.sink.type=kafka", StandardCharsets.UTF_8);

        try {
            given()
                    .when()
                    .get("/api/agent/status/test-pipeline")
                    .then()
                    .statusCode(200)
                    .body("running", equalTo(true))
                    .body("configHash", notNullValue());
        }
        finally {
            Files.deleteIfExists(configFile);
            Files.deleteIfExists(configDir);
        }
    }

    @Test
    void statusReturns404WhenContainerNotFound() {
        org.mockito.Mockito.when(docker.run(
                org.mockito.ArgumentMatchers.eq("docker"),
                org.mockito.ArgumentMatchers.eq("inspect"),
                org.mockito.ArgumentMatchers.eq("--format"),
                org.mockito.ArgumentMatchers.eq("{{.State.Running}}"),
                org.mockito.ArgumentMatchers.eq("nonexistent")))
                .thenReturn(new DockerCommandRunner.CommandOutput(1, "Error: No such object: nonexistent"));

        given()
                .when()
                .get("/api/agent/status/nonexistent")
                .then()
                .statusCode(404);
    }

    @Test
    void statusReturns200WithStoppedContainer() {
        org.mockito.Mockito.when(docker.run(
                org.mockito.ArgumentMatchers.eq("docker"),
                org.mockito.ArgumentMatchers.eq("inspect"),
                org.mockito.ArgumentMatchers.eq("--format"),
                org.mockito.ArgumentMatchers.eq("{{.State.Running}}"),
                org.mockito.ArgumentMatchers.eq("stopped-pipeline")))
                .thenReturn(new DockerCommandRunner.CommandOutput(0, "false"));

        given()
                .when()
                .get("/api/agent/status/stopped-pipeline")
                .then()
                .statusCode(200)
                .body("running", equalTo(false));
    }

    // ── Logs ──

    @Test
    void logsReturns200WithLogOutput() {
        org.mockito.Mockito.when(docker.run(
                org.mockito.ArgumentMatchers.eq("docker"),
                org.mockito.ArgumentMatchers.eq("logs"),
                org.mockito.ArgumentMatchers.eq("--tail"),
                org.mockito.ArgumentMatchers.eq("500"),
                org.mockito.ArgumentMatchers.eq("test-pipeline")))
                .thenReturn(new DockerCommandRunner.CommandOutput(0, "2026-09-01 INFO Starting Debezium Server"));

        given()
                .when()
                .get("/api/agent/logs/test-pipeline")
                .then()
                .statusCode(200)
                .body(equalTo("2026-09-01 INFO Starting Debezium Server"));
    }

    @Test
    void logsReturns500WhenContainerNotFound() {
        org.mockito.Mockito.when(docker.run(
                org.mockito.ArgumentMatchers.eq("docker"),
                org.mockito.ArgumentMatchers.eq("logs"),
                org.mockito.ArgumentMatchers.eq("--tail"),
                org.mockito.ArgumentMatchers.eq("500"),
                org.mockito.ArgumentMatchers.eq("nonexistent")))
                .thenReturn(new DockerCommandRunner.CommandOutput(1, "Error: No such container: nonexistent"));

        given()
                .when()
                .get("/api/agent/logs/nonexistent")
                .then()
                .statusCode(500);
    }
}
