/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.platform.environment.operator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;

import io.debezium.DebeziumException;
import io.debezium.operator.api.model.ConfigProperties;
import io.debezium.operator.api.model.DebeziumServer;
import io.debezium.operator.api.model.DebeziumServerBuilder;
import io.debezium.operator.api.model.DebeziumServerSpecBuilder;
import io.debezium.operator.api.model.Predicate;
import io.debezium.operator.api.model.PredicateBuilder;
import io.debezium.operator.api.model.QuarkusBuilder;
import io.debezium.operator.api.model.SinkBuilder;
import io.debezium.operator.api.model.Transformation;
import io.debezium.operator.api.model.TransformationBuilder;
import io.debezium.operator.api.model.runtime.RuntimeApiBuilder;
import io.debezium.operator.api.model.runtime.RuntimeBuilder;
import io.debezium.operator.api.model.runtime.metrics.JmxExporterBuilder;
import io.debezium.operator.api.model.runtime.metrics.MetricsBuilder;
import io.debezium.operator.api.model.source.Offset;
import io.debezium.operator.api.model.source.OffsetBuilder;
import io.debezium.operator.api.model.source.SchemaHistory;
import io.debezium.operator.api.model.source.SchemaHistoryBuilder;
import io.debezium.operator.api.model.source.SourceBuilder;
import io.debezium.operator.api.model.source.storage.CustomStoreBuilder;
import io.debezium.platform.config.PipelineConfigGroup;
import io.debezium.platform.domain.Signal;
import io.debezium.platform.domain.views.Transform;
import io.debezium.platform.domain.views.flat.PipelineFlat;
import io.debezium.platform.environment.PipelineController;
import io.debezium.platform.environment.logs.LogReader;
import io.debezium.platform.environment.operator.actions.DebeziumServerProxy;
import io.debezium.platform.environment.operator.configuration.TableNameResolver;
import io.debezium.platform.environment.operator.logs.KubernetesLogReader;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.TailPrettyLoggable;

@Dependent
public class OperatorPipelineController implements PipelineController {

    public static final String LABEL_DBZ_CONDUCTOR_ID = "debezium.io/conductor-id";
    private static final List<String> RESOLVABLE_CONFIGS = List.of("jdbc.schema.history.table.name", "jdbc.offset.table.name");
    private static final String PREDICATE_PREFIX = "p";
    private static final String PREDICATE_ALIAS_FORMAT = "%s%s";
    private static final String SIGNAL_ENABLED_CHANNELS_CONFIG = "signal.enabled.channels";
    private static final String DEFAULT_SIGNAL_CHANNELS = "source,in-process";

    private final KubernetesClient k8s;
    private final PipelineConfigGroup pipelineConfigGroup;
    private final TableNameResolver tableNameResolver;
    private final DebeziumServerProxy debeziumServerProxy;

    public OperatorPipelineController(KubernetesClient k8s,
                                      PipelineConfigGroup pipelineConfigGroup,
                                      TableNameResolver tableNameResolver,
                                      DebeziumServerProxy debeziumServerProxy) {
        this.k8s = k8s;
        this.pipelineConfigGroup = pipelineConfigGroup;
        this.tableNameResolver = tableNameResolver;
        this.debeziumServerProxy = debeziumServerProxy;
    }

    @Override
    public void deploy(PipelineFlat pipeline) {

        // Create DS quarkus configuration
        var quarkusConfig = new ConfigProperties();
        quarkusConfig.setAllProps(Map.of(
                "log.level", pipeline.getLogLevel(),
                "log.console.json", false));
        var dsQuarkus = new QuarkusBuilder()
                .withConfig(quarkusConfig)
                .build();

        var dsRuntime = new RuntimeBuilder()
                .withApi(new RuntimeApiBuilder().withEnabled().build())
                .withMetrics(new MetricsBuilder()
                        .withJmxExporter(new JmxExporterBuilder()
                                .withEnabled()
                                .build())
                        .build())
                .build();

        // Create DS source configuration
        var source = pipeline.getSource();
        var sourceConfig = new ConfigProperties();
        sourceConfig.setAllProps(source.getConfig());
        sourceConfig.setProps(SIGNAL_ENABLED_CHANNELS_CONFIG, DEFAULT_SIGNAL_CHANNELS);

        var dsSource = new SourceBuilder()
                .withSourceClass(source.getType())
                .withOffset(getOffset(pipeline))
                .withSchemaHistory(getSchemaHistory(pipeline))
                .withConfig(sourceConfig)
                .build();

        // Create DS sink configuration
        var sink = pipeline.getDestination();
        var sinkConfig = new ConfigProperties();
        sinkConfig.setAllProps(sink.getConfig());

        var dsSink = new SinkBuilder()
                .withType(sink.getType())
                .withConfig(sinkConfig)
                .build();

        List<Transformation> transformations = pipeline.getTransforms().stream()
                .map(this::buildTransformation)
                .toList();

        Map<String, Predicate> predicates = pipeline.getTransforms().stream()
                .collect(Collectors.toMap(
                        this::getPredicateName,
                        this::buildPredicate));

        // Create DS resource
        var ds = new DebeziumServerBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(pipeline.getName())
                        .withLabels(Map.of(LABEL_DBZ_CONDUCTOR_ID, pipeline.getId().toString()))
                        .build())
                .withSpec(new DebeziumServerSpecBuilder()
                        .withQuarkus(dsQuarkus)
                        .withRuntime(dsRuntime)
                        .withSource(dsSource)
                        .withSink(dsSink)
                        .withTransforms(transformations)
                        .withPredicates(predicates)
                        .build())
                .build();

        // apply to server
        k8s.resource(ds).serverSideApply();
    }

    private Predicate buildPredicate(Transform transform) {

        var predicateConfig = new ConfigProperties();
        predicateConfig.setAllProps(transform.getPredicate().getConfig());

        return new PredicateBuilder()
                .withType(transform.getPredicate().getType())
                .withConfig(predicateConfig)
                .build();
    }

    private Transformation buildTransformation(Transform transform) {

        var transformConfig = new ConfigProperties();
        transformConfig.setAllProps(transform.getConfig());

        return new TransformationBuilder()
                .withType(transform.getType())
                .withConfig(transformConfig)
                .withPredicate(getPredicateName(transform))
                .withNegate(transform.getPredicate().isNegate())
                .build();

    }

    private String getPredicateName(Transform transform) {
        return String.format(PREDICATE_ALIAS_FORMAT, PREDICATE_PREFIX, transform.getId());
    }

    private SchemaHistory getSchemaHistory(PipelineFlat pipeline) {

        var pipelineSchemaHistoryConfigs = pipelineConfigGroup.schema().config();
        var schemaHistoryType = pipelineConfigGroup.schema().internal();

        Map<String, String> schemaHistoryStorageConfigs = new HashMap<>(pipelineSchemaHistoryConfigs);
        ConfigProperties schemaHistoryProps = new ConfigProperties();
        schemaHistoryStorageConfigs.forEach(schemaHistoryProps::setProps);

        RESOLVABLE_CONFIGS.forEach(prop -> schemaHistoryProps.setProps(prop, tableNameResolver.resolve(pipeline, schemaHistoryStorageConfigs.get(prop))));

        return new SchemaHistoryBuilder().withStore(new CustomStoreBuilder()
                .withType(schemaHistoryType)
                .withConfig(schemaHistoryProps)
                .build()).build();
    }

    private Offset getOffset(PipelineFlat pipeline) {

        var pipelineOffsetConfigs = pipelineConfigGroup.offset().storage().config();
        var offsetType = pipelineConfigGroup.offset().storage().type();

        Map<String, String> offsetStorageConfigs = new HashMap<>(pipelineOffsetConfigs);
        ConfigProperties offsetStorageProps = new ConfigProperties();
        offsetStorageConfigs.forEach(offsetStorageProps::setProps);

        RESOLVABLE_CONFIGS.forEach(prop -> offsetStorageProps.setProps(prop, tableNameResolver.resolve(pipeline, pipelineOffsetConfigs.get(prop))));

        return new OffsetBuilder().withStore(new CustomStoreBuilder()
                .withType(offsetType)
                .withConfig(offsetStorageProps)
                .build()).build();
    }

    @Override
    public void undeploy(Long id) {
        k8s.resources(DebeziumServer.class)
                .withLabels(Map.of(LABEL_DBZ_CONDUCTOR_ID, id.toString()))
                .delete();
    }

    @Override
    public void stop(Long id) {
        stop(id, true);
    }

    @Override
    public void start(Long id) {
        stop(id, false);
    }

    public Optional<DebeziumServer> findById(Long id) {
        return k8s.resources(DebeziumServer.class)
                .withLabels(Map.of(LABEL_DBZ_CONDUCTOR_ID, id.toString()))
                .list()
                .getItems()
                .stream()
                .findFirst();
    }

    private TailPrettyLoggable findDeploymentLoggable(Long id) {
        return findById(id)
                .map(DebeziumServer::getMetadata)
                .map(ObjectMeta::getName)
                .map(name -> k8s.apps().deployments().withName(name))
                .get();
    }

    @Override
    public LogReader logReader(Long id) {
        return new KubernetesLogReader(() -> findDeploymentLoggable(id));
    }

    @Override
    public void sendSignal(Long pipelineId, Signal signal) {
        findById(pipelineId).ifPresentOrElse(
                debeziumServer -> debeziumServerProxy.sendSignal(signal, debeziumServer),
                () -> {
                    throw new DebeziumException(String.format("Pipeline with id %s not found", pipelineId));
                });
    }

    private void stop(Long id, boolean stop) {
        findById(id).ifPresent(ds -> {
            ds.setStopped(stop);
            k8s.resource(ds).serverSideApply();
        });
    }
}
