/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.data.dto;

import java.util.Map;
import java.util.Objects;

public record SignalRequest(String id, String type, String data, Map<String, Object> additionalData) {
    // TODO add validation

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
