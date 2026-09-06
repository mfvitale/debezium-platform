/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.environment.host;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.debezium.platform.domain.HostAllocation;
import io.debezium.platform.domain.HostDeploymentService;
import io.debezium.platform.domain.views.HostDeployment;
import io.debezium.platform.environment.host.agent.HostAgentClient;

/**
 * Agent-based implementation of {@link HostContainerRuntime}.
 *
 * <p>Replaces {@link AnsibleContainerRuntime} by delegating container
 * lifecycle operations to the remote Host Agent via REST calls. The
 * Agent runs on each provisioned host and translates HTTP requests
 * into Docker CLI commands locally.
 *
 * <p>Marked as {@code @Alternative} with {@code @Priority(1)} so it
 * takes precedence over {@code AnsibleContainerRuntime} in CDI
 * resolution. This follows the Quarkus CDI pattern for swapping
 * implementations without removing the old one.
 *
 * <p>The {@code deploy} method receives the full allocation (with
 * hostname, port, token) directly. The {@code undeploy/stop/start/logs}
 * methods resolve the host details from the deployment record via
 * {@link HostDeploymentService}.
 *
 * @see HostContainerRuntime
 * @see AnsibleContainerRuntime
 * @see HostAgentClient
 *
 * @author Divyanshu Kumar Nayak
 */
@ApplicationScoped
@Alternative
@jakarta.annotation.Priority(1)
public class AgentContainerRuntime implements HostContainerRuntime {

    private final Logger logger;
    private final HostAgentClient agentClient;
    private final HostDeploymentService deploymentService;

    @Inject
    public AgentContainerRuntime(Logger logger,
                                 HostAgentClient agentClient,
                                 HostDeploymentService deploymentService) {
        this.logger = logger;
        this.agentClient = agentClient;
        this.deploymentService = deploymentService;
    }

    @Override
    public void deploy(HostAllocation allocation, String containerName,
                       String configContent, String image) {
        String sshAlias = allocation.host().getSshAlias();
        int port = allocation.allocatedPort();

        // Resolve hostname, agentPort, and agentToken from the host status.
        // The HostAllocation only carries a HostStatusReference (sshAlias + id).
        // We need the full host record to reach the Agent.
        HostDeployment deployment = deploymentService.findByContainerName(containerName);
        if (deployment == null) {
            // Fallback: query by pipeline via the allocation.
            // This can happen during the first deploy when the HostDeployment
            // row has just been created but not yet fetched.
            logger.debugv("No deployment found by containerName {0}, looking up host details by sshAlias", containerName);
        }

        String hostname = deployment != null ? deployment.getHostname() : resolveHostname(sshAlias);
        int agentPort = deployment != null ? deployment.getAgentPort() : 8090;
        String agentToken = deployment != null ? deployment.getAgentToken() : "";

        agentClient.deploy(hostname, agentPort, agentToken,
                containerName, image, port, configContent);

        logger.infov("Deploy request sent to Agent on {0} for container {1}, port {2}",
                sshAlias, containerName, port);
    }

    @Override
    public void undeploy(String host, String containerName) {
        HostDeployment deployment = deploymentService.findByContainerName(containerName);
        if (deployment == null) {
            logger.warnv("No deployment found for container {0} on {1}, skipping Agent undeploy", containerName, host);
            return;
        }
        agentClient.undeploy(deployment.getHostname(), deployment.getAgentPort(),
                deployment.getAgentToken(), containerName);
    }

    @Override
    public void stop(String host, String containerName) {
        HostDeployment deployment = deploymentService.findByContainerName(containerName);
        if (deployment == null) {
            logger.warnv("No deployment found for container {0}, cannot stop via Agent", containerName);
            return;
        }
        agentClient.stop(deployment.getHostname(), deployment.getAgentPort(),
                deployment.getAgentToken(), containerName);
    }

    @Override
    public void start(String host, String containerName) {
        HostDeployment deployment = deploymentService.findByContainerName(containerName);
        if (deployment == null) {
            logger.warnv("No deployment found for container {0}, cannot start via Agent", containerName);
            return;
        }
        agentClient.start(deployment.getHostname(), deployment.getAgentPort(),
                deployment.getAgentToken(), containerName);
    }

    @Override
    public String logs(String host, String containerName) {
        HostDeployment deployment = deploymentService.findByContainerName(containerName);
        if (deployment == null) {
            return "[No deployment record found for container " + containerName + "]";
        }
        return agentClient.logs(deployment.getHostname(), deployment.getAgentPort(),
                deployment.getAgentToken(), containerName);
    }

    /**
     * Fallback hostname resolution when the deployment record is not yet available.
     * This should rarely happen — only during the brief window between
     * {@code createDeployment()} and the actual deploy call.
     */
    private String resolveHostname(String sshAlias) {
        // In the common case, sshAlias IS the hostname
        return sshAlias;
    }
}
