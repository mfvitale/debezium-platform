/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.agent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;

/**
 * REST API for managing Docker containers on the local host.
 *
 * <p>This is the host-path equivalent of the Debezium Operator — the
 * "person on the other side" that receives instructions from the
 * Conductor and manages Docker containers on the remote host.
 *
 * <p>Endpoints mirror the operations defined in {@code HostContainerRuntime}
 * on the Conductor side:
 * <ul>
 *   <li>{@code POST /deploy}  — starts a Debezium Server container (async, 202)</li>
 *   <li>{@code POST /undeploy} — removes a container ({@code docker rm -f})</li>
 *   <li>{@code POST /stop}     — gracefully stops a container</li>
 *   <li>{@code POST /start}    — starts a stopped container</li>
 *   <li>{@code GET  /status/{name}} — container running state + config hash</li>
 *   <li>{@code GET  /logs/{name}}   — last 500 lines of container logs</li>
 * </ul>
 *
 * @see DockerCommandRunner
 * @see AgentTokenFilter
 */
@jakarta.ws.rs.Path("/api/agent")
public class AgentResource {

    private static final String CONFIG_FILE_NAME = "application.properties";
    private static final String PATH_SEPARATOR = "/";

    private final Logger logger;
    private final DockerCommandRunner docker;
    private final AgentConfig config;

    public AgentResource(Logger logger, DockerCommandRunner docker, AgentConfig config) {
        this.logger = logger;
        this.docker = docker;
        this.config = config;
    }

    /**
     * Deploys a Debezium Server container.
     *
     * <p>Writes the {@code application.properties} config file to disk,
     * creates the data directory, and starts the container asynchronously.
     * Returns {@code 202 Accepted} immediately — the Conductor's status
     * poller will detect when the container is running.
     *
     * <p><strong>Data directory ownership:</strong> The data directory is
     * created by the Host Agent (running as {@code root}) and immediately
     * reassigned to {@code 185:0} (the {@code jboss} user inside the official
     * {@code quay.io/debezium/server} image) via {@code chown}. The container
     * is started with {@code --user 185:0} so the Debezium Server process
     * runs as UID 185, which owns the data directory and can write
     * {@code offsets.dat} and {@code schema-history.dat} without requiring
     * world-writable ({@code 777}) permissions.
     */
    @POST
    @jakarta.ws.rs.Path("/deploy")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deploy(DeployRequest request) {
        String containerName = request.containerName();
        logger.infov("Deploy request received for container {0}, image {1}, port {2}",
                containerName, request.image(), request.port());

        try {
            String configDir = config.configBasePath() + PATH_SEPARATOR + containerName;
            String configPath = configDir + PATH_SEPARATOR + CONFIG_FILE_NAME;
            String dataDir = config.dataBasePath() + PATH_SEPARATOR + containerName;

            // Create directories
            Files.createDirectories(Path.of(configDir));
            Files.createDirectories(Path.of(dataDir));

            // Assign data directory ownership to UID 185 (Debezium Server jboss user).
            // The Host Agent runs as root so it can create /opt/debezium/data/, but the
            // Debezium Server container runs as UID 185. Without chown, UID 185 gets
            // only 'r-x' (group/other bits of a root-owned 755 dir) and cannot write
            // offsets.dat or schema-history.dat, causing AccessDeniedException.
            // Setting owner=185:0 keeps mode 0755 while giving UID 185 write access
            // as the directory owner — no insecure world-write (777) needed.
            new ProcessBuilder("chown", "185:0", dataDir)
                    .start()
                    .waitFor();

            // Write config file
            Files.writeString(Path.of(configPath), request.configContent(), StandardCharsets.UTF_8);
            logger.infov("Config written to {0}", configPath);

            // Remove any stale container with the same name (idempotent redeploy)
            docker.run("docker", "rm", "-f", containerName);

            // Start container asynchronously — docker pull can be slow.
            // --user 185:0 makes the Debezium Server process run as UID 185
            // (the jboss user), which owns the bind-mounted data directory
            // (chown'd above). This matches the identity used inside the
            // official quay.io/debezium/server image and avoids world-writable
            // directory permissions.
            docker.runAsync(
                    "docker", "run", "-d",
                    "--name", containerName,
                    "--user", "185:0",
                    "-p", request.port() + ":8080",
                    "-v", configPath + ":/debezium/config/application.properties",
                    "-v", dataDir + ":/debezium/data",
                    request.image());

            return Response.accepted().build();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.errorv(e, "Interrupted while preparing deploy for container {0}", containerName);
            return Response.serverError()
                    .entity("Deployment interrupted: " + e.getMessage())
                    .build();
        }
        catch (IOException e) {
            logger.errorv(e, "Failed to prepare deploy for container {0}", containerName);
            return Response.serverError()
                    .entity("Failed to prepare deployment: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Force-removes a container. Idempotent — succeeds even if the
     * container does not exist.
     */
    @POST
    @jakarta.ws.rs.Path("/undeploy")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response undeploy(ContainerNameRequest request) {
        String containerName = request.containerName();
        logger.infov("Undeploy request received for container {0}", containerName);

        docker.run("docker", "rm", "-f", containerName);

        // Clean up config and data directories
        cleanupContainerFiles(containerName);

        return Response.ok().build();
    }

    /**
     * Gracefully stops a running container.
     */
    @POST
    @jakarta.ws.rs.Path("/stop")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response stop(ContainerNameRequest request) {
        String containerName = request.containerName();
        logger.infov("Stop request received for container {0}", containerName);

        DockerCommandRunner.CommandOutput result = docker.run("docker", "stop", containerName);
        if (!result.isSuccess()) {
            return Response.serverError()
                    .entity("Failed to stop container: " + result.output())
                    .build();
        }

        return Response.ok().build();
    }

    /**
     * Starts a previously stopped container.
     */
    @POST
    @jakarta.ws.rs.Path("/start")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response start(ContainerNameRequest request) {
        String containerName = request.containerName();
        logger.infov("Start request received for container {0}", containerName);

        DockerCommandRunner.CommandOutput result = docker.run("docker", "start", containerName);
        if (!result.isSuccess()) {
            return Response.serverError()
                    .entity("Failed to start container: " + result.output())
                    .build();
        }

        return Response.ok().build();
    }

    /**
     * Returns the running state and config hash for a container.
     *
     * <p>Returns {@code 404 Not Found} if the container does not exist,
     * allowing the Conductor to distinguish "container was removed" from
     * "container exists but is stopped."
     */
    @GET
    @jakarta.ws.rs.Path("/status/{containerName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response status(@PathParam("containerName") String containerName) {
        // Check if container exists at all
        DockerCommandRunner.CommandOutput inspectResult = docker.run(
                "docker", "inspect", "--format", "{{.State.Running}}", containerName);

        if (!inspectResult.isSuccess()) {
            String output = inspectResult.output() != null ? inspectResult.output().toLowerCase() : "";
            if (output.contains("no such object") || output.contains("no such container")) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Container not found: " + containerName)
                        .build();
            }
            return Response.serverError()
                    .entity("Failed to inspect container: " + inspectResult.output())
                    .build();
        }

        boolean running = inspectResult.output().trim().equalsIgnoreCase("true");
        String configHash = computeConfigHash(containerName);

        return Response.ok(new ContainerStatus(running, configHash)).build();
    }

    /**
     * Returns the last 500 lines of container logs as plain text.
     */
    @GET
    @jakarta.ws.rs.Path("/logs/{containerName}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response logs(@PathParam("containerName") String containerName) {
        DockerCommandRunner.CommandOutput result = docker.run(
                "docker", "logs", "--tail", "500", containerName);

        if (!result.isSuccess()) {
            return Response.serverError()
                    .entity("Failed to retrieve logs: " + result.output())
                    .build();
        }

        return Response.ok(result.output()).build();
    }

    /**
     * Computes the SHA-256 hash of the deployed {@code application.properties}
     * file for drift detection. Returns {@code null} if the file does not exist.
     */
    private String computeConfigHash(String containerName) {
        Path configPath = Path.of(config.configBasePath(), containerName, CONFIG_FILE_NAME);

        if (!Files.exists(configPath)) {
            return null;
        }

        try {
            byte[] content = Files.readAllBytes(configPath);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            return HexFormat.of().formatHex(hash);
        }
        catch (IOException | NoSuchAlgorithmException e) {
            logger.warnv(e, "Failed to compute config hash for container {0}", containerName);
            return null;
        }
    }

    /**
     * Cleans up config and data directories for a container on undeploy.
     */
    private void cleanupContainerFiles(String containerName) {
        try {
            Path configDir = Path.of(config.configBasePath(), containerName);
            Path dataDir = Path.of(config.dataBasePath(), containerName);

            deleteDirectoryRecursively(configDir);
            deleteDirectoryRecursively(dataDir);
        }
        catch (Exception e) {
            logger.warnv(e, "Failed to clean up files for container {0}, proceeding", containerName);
        }
    }

    private void deleteDirectoryRecursively(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (var walker = Files.walk(dir)) {
                walker.sorted(java.util.Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            }
                            catch (IOException e) {
                                logger.debugv("Could not delete {0}: {1}", path, e.getMessage());
                            }
                        });
            }
        }
    }

    /**
     * Request body for endpoints that only need a container name.
     */
    public record ContainerNameRequest(String containerName) {
    }
}
