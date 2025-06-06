/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.environment;

public interface EnvironmentController {

    PipelineController pipelines();

    VaultController vaults();

}
