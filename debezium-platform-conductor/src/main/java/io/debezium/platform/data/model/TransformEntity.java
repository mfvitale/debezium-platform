/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.data.model;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.validation.constraints.NotEmpty;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity(name = "transform")
public class TransformEntity {
    @Id
    @GeneratedValue
    private Long id;
    @NotEmpty
    @Column(unique = true, nullable = false)
    private String name;
    private String description;
    @NotEmpty
    @Column(nullable = false)
    private String type;
    @NotEmpty
    @Column(nullable = false)
    private String schema;
    @ManyToMany
    @JoinTable(joinColumns = @JoinColumn(name = "transform_id"), inverseJoinColumns = @JoinColumn(name = "vault_id"))
    private Set<VaultEntity> vaults = new HashSet<>();
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> config;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "type", column = @Column(name = "predicate_type")),
            @AttributeOverride(name = "config", column = @Column(name = "predicate_config")),
            @AttributeOverride(name = "negate", column = @Column(name = "predicate_negate")),
    })
    private Predicate predicate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public Set<VaultEntity> getVaults() {
        return vaults;
    }

    public void setVaults(Set<VaultEntity> vaults) {
        this.vaults = vaults;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public Predicate getPredicate() {
        return predicate;
    }

    public void setPredicate(Predicate predicate) {
        this.predicate = predicate;
    }

    @Embeddable
    public static class Predicate {

        public Predicate() {
        }

        private String type;

        @JdbcTypeCode(SqlTypes.JSON)
        private Map<String, Object> config;

        private boolean negate;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Map<String, Object> getConfig() {
            return config;
        }

        public void setConfig(Map<String, Object> config) {
            this.config = config;
        }

        public boolean isNegate() {
            return negate;
        }

        public void setNegate(boolean negate) {
            this.negate = negate;
        }
    }
}
