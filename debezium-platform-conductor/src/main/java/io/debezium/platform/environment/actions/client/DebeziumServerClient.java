/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.environment.actions.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.debezium.platform.domain.Signal;
import io.quarkus.rest.client.reactive.Url;

@Path("/api")
@RegisterRestClient(configKey = "debezium-server-api")
public interface DebeziumServerClient {

    @POST
    @Path("/signals")
    @Consumes(MediaType.APPLICATION_JSON)
    Response sendSignal(@Url String url, Signal body);
}
