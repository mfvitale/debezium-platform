/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.data.dto;

public record SignalResponse(Signal signal) {

    public record Signal(String id) {

    }

    public static SignalResponse from(String id) {
        return new SignalResponse(new Signal(id));
    }
}
