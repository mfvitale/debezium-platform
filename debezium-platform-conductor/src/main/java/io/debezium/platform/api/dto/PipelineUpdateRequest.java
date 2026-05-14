/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.api.dto;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotEmpty;

public record PipelineUpdateRequest(
        @NotEmpty String name,
        String description,
        List<NamedRef> transforms,
        @NotEmpty String logLevel,
        Map<String, String> logLevels) {
}
