/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.environment.actions;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.sql.Statement;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.agroal.api.AgroalDataSource;
import io.debezium.platform.environment.actions.db.MariaDbTestResource;
import io.debezium.platform.environment.actions.db.MySQLTestResource;
import io.debezium.platform.environment.actions.db.OracleTestResource;
import io.debezium.platform.environment.actions.db.PostgresTestResource;
import io.debezium.platform.environment.actions.db.SqlserverTestResource;
import io.quarkus.agroal.DataSource;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(DatabaseTestProfile.class)
@QuarkusTestResource(value = PostgresTestResource.class, restrictToAnnotatedClass = true)
@QuarkusTestResource(value = MySQLTestResource.class, restrictToAnnotatedClass = true)
@QuarkusTestResource(value = MariaDbTestResource.class, restrictToAnnotatedClass = true)
@QuarkusTestResource(value = OracleTestResource.class, restrictToAnnotatedClass = true)
@QuarkusTestResource(value = SqlserverTestResource.class, restrictToAnnotatedClass = true)
public class SignalDataCollectionCheckerIT {

    // InjectableInstance is required when multiple datasource must be active at runtime
    // see https://quarkus.io/guides/datasource#configure-multiple-datasources
    @Inject
    InjectableInstance<AgroalDataSource> postgresDataSource;

    @Inject
    @DataSource("mysql")
    InjectableInstance<AgroalDataSource> mysqlDataSource;

    @Inject
    @DataSource("mariadb")
    InjectableInstance<AgroalDataSource> mariadbDataSource;

    @Inject
    @DataSource("oracle")
    InjectableInstance<AgroalDataSource> oracleDataSource;

    @Inject
    @DataSource("mssql")
    InjectableInstance<AgroalDataSource> mssqlDataSource;

    @Test
    void testVerifySchemaOnPostgres() throws SQLException {

        AgroalDataSource dataSource = postgresDataSource.get();

        SignalDataCollectionChecker verifier = new SignalDataCollectionChecker();

        assertThat(verifier.verifyTableStructure(dataSource.getConnection(), "test", "public", "debezium_signal")).isFalse();

        try (Statement statement = dataSource.getConnection().createStatement()) {

            statement.execute("CREATE TABLE public.debezium_signal (id VARCHAR(42) PRIMARY KEY, type VARCHAR(32) NOT NULL, data VARCHAR(2048) NULL);");
        }

        assertThat(verifier.verifyTableStructure(dataSource.getConnection(), "test", "public", "debezium_signal")).isTrue();

    }

    @Test
    void testVerifySchemaOnMySQL() throws SQLException {

        AgroalDataSource dataSource = mysqlDataSource.get();

        SignalDataCollectionChecker verifier = new SignalDataCollectionChecker();

        assertThat(verifier.verifyTableStructure(dataSource.getConnection(), "test", "", "debezium_signal")).isFalse();

        try (Statement statement = dataSource.getConnection().createStatement()) {

            statement.execute("CREATE TABLE test.debezium_signal (id VARCHAR(42) PRIMARY KEY, type VARCHAR(32) NOT NULL, data VARCHAR(2048) NULL);");
        }

        assertThat(verifier.verifyTableStructure(dataSource.getConnection(), "test", "", "debezium_signal")).isTrue();

    }

    @Test
    void testVerifySchemaOnMariaDb() throws SQLException {

        AgroalDataSource dataSource = mariadbDataSource.get();

        SignalDataCollectionChecker verifier = new SignalDataCollectionChecker();

        assertThat(verifier.verifyTableStructure(dataSource.getConnection(), "test", "", "debezium_signal")).isFalse();

        try (Statement statement = dataSource.getConnection().createStatement()) {

            statement.execute("CREATE TABLE test.debezium_signal (id VARCHAR(42) PRIMARY KEY, type VARCHAR(32) NOT NULL, data VARCHAR(2048) NULL);");
        }

        assertThat(verifier.verifyTableStructure(dataSource.getConnection(), "test", "", "debezium_signal")).isTrue();

    }

    @Test
    void testVerifySchemaOnOracle() throws SQLException {

        AgroalDataSource dataSource = oracleDataSource.get();

        SignalDataCollectionChecker verifier = new SignalDataCollectionChecker();

        assertThat(verifier.verifyTableStructure(dataSource.getConnection(), null, "DEBEZIUM", "DEBEZIUM_SIGNAL")).isFalse();

        try (Statement statement = dataSource.getConnection().createStatement()) {

            statement.execute("CREATE TABLE DEBEZIUM.debezium_signal (id VARCHAR2(42) PRIMARY KEY, type VARCHAR2(32) NOT NULL, data VARCHAR2(2048) NULL)");
        }

        assertThat(verifier.verifyTableStructure(dataSource.getConnection(), null, "DEBEZIUM", "DEBEZIUM_SIGNAL")).isTrue();

    }

    @Test
    void testVerifySchemaOnMSSQL() throws SQLException {

        AgroalDataSource dataSource = mssqlDataSource.get();

        SignalDataCollectionChecker verifier = new SignalDataCollectionChecker();

        assertThat(verifier.verifyTableStructure(dataSource.getConnection(), "master", "dbo", "debezium_signal")).isFalse();

        try (Statement statement = dataSource.getConnection().createStatement()) {

            statement.execute("CREATE TABLE dbo.debezium_signal (id VARCHAR(42) PRIMARY KEY,type VARCHAR(32) NOT NULL,data VARCHAR(2048) NULL);");
        }

        assertThat(verifier.verifyTableStructure(dataSource.getConnection(), "master", "dbo", "debezium_signal")).isTrue();

    }
}
