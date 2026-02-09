/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class CatalogResourceIT {

    @Test
    public void testGetCatalogForVersion() {
        given()
                .pathParam("version", "3.5.0-SNAPSHOT")
                .when()
                .get("api/catalog/{version}")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("schemaVersion", notNullValue())
                .body("build", notNullValue())
                .body("build.timestamp", notNullValue())
                .body("build.sourceRepository", notNullValue())
                .body("build.sourceCommit", notNullValue())
                .body("components", notNullValue())
                .body("components.keySet()", not(empty()))
                .body("components.keySet()", hasItem("source-connector"))
                .body("components.keySet()", hasItem("sink-connector"))
                .body("components.keySet()", hasItem("transformation"));
    }

    @Test
    public void testGetCatalogWithSourceConnectorType() {
        given()
                .pathParam("version", "3.5.0-SNAPSHOT")
                .queryParam("type", "source-connector")
                .when()
                .get("api/catalog/{version}")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("schemaVersion", notNullValue())
                .body("components", notNullValue())
                .body("components.keySet()", hasSize(1))
                .body("components.keySet()", hasItem("source-connector"))
                .body("components.'source-connector'", not(empty()))
                .body("components.'source-connector'[0].class", notNullValue())
                .body("components.'source-connector'[0].name", notNullValue())
                .body("components.'source-connector'[0].description", notNullValue())
                .body("components.'source-connector'[0].descriptor", notNullValue());
    }

    @Test
    public void testGetCatalogWithSinkConnectorType() {
        given()
                .pathParam("version", "3.5.0-SNAPSHOT")
                .queryParam("type", "sink-connector")
                .when()
                .get("api/catalog/{version}")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("components.keySet()", hasSize(1))
                .body("components.keySet()", hasItem("sink-connector"))
                .body("components.'sink-connector'", not(empty()))
                .body("components.'sink-connector'[0].class", notNullValue())
                .body("components.'sink-connector'[0].name", notNullValue());
    }

    @Test
    public void testGetCatalogWithTransformationType() {
        given()
                .pathParam("version", "3.5.0-SNAPSHOT")
                .queryParam("type", "transformation")
                .when()
                .get("api/catalog/{version}")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("components.keySet()", hasSize(1))
                .body("components.keySet()", hasItem("transformation"))
                .body("components.'transformation'", not(empty()))
                .body("components.'transformation'[0].class", notNullValue())
                .body("components.'transformation'[0].name", notNullValue());
    }

    @Test
    public void testGetCatalogWithInvalidVersion() {
        given()
                .pathParam("version", "99.99.99")
                .when()
                .get("api/catalog/{version}")
                .then()
                .statusCode(404);
    }

    @Test
    public void testGetCatalogWithInvalidType() {
        given()
                .pathParam("version", "3.5.0-SNAPSHOT")
                .queryParam("type", "invalid-type")
                .when()
                .get("api/catalog/{version}")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("components.keySet()", hasSize(0));
    }

    @Test
    public void testGetSourceConnectorDescriptor() {
        given()
                .pathParam("version", "3.5.0-SNAPSHOT")
                .pathParam("type", "source-connector")
                .pathParam("class", "io.debezium.connector.mysql.MySqlConnector")
                .when()
                .get("api/catalog/{version}/{type}/{class}")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("name", notNullValue())
                .body("type", notNullValue())
                .body("version", notNullValue())
                .body("metadata", notNullValue())
                .body("metadata.description", notNullValue())
                .body("properties", not(empty()))
                .body("properties[0].name", notNullValue())
                .body("properties[0].type", notNullValue())
                .body("properties[0].display", notNullValue())
                .body("groups", not(empty()))
                .body("groups[0].name", notNullValue());
    }

    @Test
    public void testGetPostgresConnectorDescriptor() {
        given()
                .pathParam("version", "3.5.0-SNAPSHOT")
                .pathParam("type", "source-connector")
                .pathParam("class", "io.debezium.connector.postgresql.PostgresConnector")
                .when()
                .get("api/catalog/{version}/{type}/{class}")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("name", notNullValue())
                .body("type", notNullValue())
                .body("version", notNullValue())
                .body("properties", not(empty()));
    }

    @Test
    public void testGetSinkConnectorDescriptor() {
        given()
                .pathParam("version", "3.5.0-SNAPSHOT")
                .pathParam("type", "sink-connector")
                .pathParam("class", "io.debezium.connector.jdbc.JdbcSinkConnector")
                .when()
                .get("api/catalog/{version}/{type}/{class}")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("name", notNullValue())
                .body("type", notNullValue())
                .body("version", notNullValue())
                .body("properties", not(empty()));
    }

    @Test
    public void testGetTransformationDescriptor() {
        given()
                .pathParam("version", "3.5.0-SNAPSHOT")
                .pathParam("type", "transformation")
                .pathParam("class", "io.debezium.transforms.ExtractNewRecordState")
                .when()
                .get("api/catalog/{version}/{type}/{class}")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("name", notNullValue())
                .body("type", notNullValue())
                .body("version", notNullValue())
                .body("properties", not(empty()))
                .body("properties[0].name", notNullValue());
    }

    @Test
    public void testGetDescriptorWithInvalidVersion() {
        given()
                .pathParam("version", "99.99.99")
                .pathParam("type", "source-connector")
                .pathParam("class", "io.debezium.connector.mysql.MySqlConnector")
                .when()
                .get("api/catalog/{version}/{type}/{class}")
                .then()
                .statusCode(404);
    }

    @Test
    public void testGetDescriptorWithInvalidType() {
        given()
                .pathParam("version", "3.5.0-SNAPSHOT")
                .pathParam("type", "invalid-type")
                .pathParam("class", "io.debezium.connector.mysql.MySqlConnector")
                .when()
                .get("api/catalog/{version}/{type}/{class}")
                .then()
                .statusCode(404);
    }

    @Test
    public void testGetDescriptorWithInvalidClass() {
        given()
                .pathParam("version", "3.5.0-SNAPSHOT")
                .pathParam("type", "source-connector")
                .pathParam("class", "io.debezium.connector.InvalidConnector")
                .when()
                .get("api/catalog/{version}/{type}/{class}")
                .then()
                .statusCode(404);
    }

    @Test
    public void testGetCatalogMetadata() {
        given()
                .pathParam("version", "3.5.0-SNAPSHOT")
                .when()
                .get("api/catalog/{version}")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("components.'source-connector'", not(empty()))
                .body("components.'source-connector'[0].descriptor", notNullValue())
                .body("components.'sink-connector'", not(empty()))
                .body("components.'transformation'", not(empty()));
    }

    @Test
    public void testCatalogComponentsStructure() {
        given()
                .pathParam("version", "3.5.0-SNAPSHOT")
                .when()
                .get("api/catalog/{version}")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("components.'source-connector'[0].class", notNullValue())
                .body("components.'source-connector'[0].name", notNullValue())
                .body("components.'source-connector'[0].description", notNullValue())
                .body("components.'source-connector'[0].descriptor", notNullValue());
    }
}
