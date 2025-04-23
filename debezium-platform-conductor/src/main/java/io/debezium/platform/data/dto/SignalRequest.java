package io.debezium.platform.data.dto;

import java.util.Map;

public record SignalRequest(String id, String type, String data, Map<String, Object> additionalData) {
    // TODO add validation
}
