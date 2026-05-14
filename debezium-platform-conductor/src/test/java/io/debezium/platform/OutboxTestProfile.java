/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform;

import io.quarkus.test.junit.QuarkusTestProfile;

public class OutboxTestProfile implements QuarkusTestProfile {

    @Override
    public String getConfigProfile() {
        return "test";
    }
}
