/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.domain;

import java.util.Map;

public record Signal(String id, String type, String data, Map<String, Object> stringObjectMap) {
}
