/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.agent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

/**
 * Executes Docker CLI commands via {@link ProcessBuilder}.
 *
 * <p>This follows the same {@code ProcessBuilder} pattern used by
 * {@code AnsibleHostProvisioner} in the Conductor module. Each method
 * constructs a process, captures stdout + stderr, and returns a
 * {@link CommandOutput} with the exit code and combined output.
 *
 * <p>Docker commands run locally on the host where the Agent is deployed,
 * so no SSH or Ansible is involved — just a direct {@code docker} CLI call.
 */
@ApplicationScoped
public class DockerCommandRunner {

    private static final int DEFAULT_TIMEOUT_SECONDS = 120;

    private final Logger logger;

    public DockerCommandRunner(Logger logger) {
        this.logger = logger;
    }

    /**
     * Runs a Docker command synchronously and returns its output.
     *
     * @param command  the command and arguments (e.g. {@code "docker", "inspect", "--format", "...", "name"})
     * @return the command output with exit code and stdout/stderr
     */
    public CommandOutput run(String... command) {
        try {
            logger.debugv("Executing: {0}", String.join(" ", command));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }

            boolean finished = process.waitFor(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new CommandOutput(124, "Command timed out after " + DEFAULT_TIMEOUT_SECONDS + " seconds");
            }

            int exitCode = process.exitValue();
            logger.debugv("Command exited with code {0}: {1}", exitCode, output);
            return new CommandOutput(exitCode, output);
        }
        catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logger.errorv(e, "Failed to execute command: {0}", String.join(" ", command));
            return new CommandOutput(-1, "Process execution failed: " + e.getMessage());
        }
    }

    /**
     * Fires a Docker command asynchronously (fire-and-forget).
     *
     * <p>Used for {@code docker run} during deploy — the Agent returns
     * {@code 202 Accepted} immediately while Docker pulls the image and
     * starts the container in the background. The Conductor's status
     * poller will detect when the container is running.
     *
     * @param command  the command and arguments
     */
    public void runAsync(String... command) {
        Thread.ofVirtual().name("docker-async").start(() -> {
            CommandOutput result = run(command);
            if (result.exitCode() != 0) {
                logger.warnv("Async Docker command failed (exit={0}): {1}",
                        result.exitCode(), result.output());
            }
        });
    }

    /**
     * Output of a Docker CLI command.
     *
     * @param exitCode  process exit code (0 = success)
     * @param output    combined stdout + stderr
     */
    public record CommandOutput(int exitCode, String output) {

        public boolean isSuccess() {
            return exitCode == 0;
        }
    }
}
