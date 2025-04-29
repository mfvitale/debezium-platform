/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.data.dto;

import java.util.Map;
import java.util.Objects;

import jakarta.validation.constraints.NotEmpty;

public record SignalRequest(@NotEmpty String id,
        @NotEmpty String type,
        @NotEmpty String data,
        Map<String, Object> additionalData) {

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SignalRequest that = (SignalRequest) o;
        return Objects.equals(id, that.id) && Objects.equals(type, that.type) && Objects.equals(data, that.data) && Objects.equals(
                additionalData, that.additionalData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, data, additionalData);
    }
}
