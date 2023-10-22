package com.example.kafkaconnect.strimzi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.images.builder.Transferable;

import java.util.Map;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static java.util.Map.entry;

@Configuration
@AutoConfigureOrder
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(EmbeddedStrimziKafkaBootstrapConfig.class)
@EnableConfigurationProperties(KafkaConnectProperties.class)
@Slf4j
public class EmbeddedStrimziKafkaConnectBootstrapConfig {

    private static final String KAFKA_CONNECT_HOST_NAME = "kafka-connect.testcontainer.docker";

    @Bean(name = "kafka-connect-confluent", destroyMethod = "stop")
    public GenericContainer<?> kafkaConnect(
            ConfigurableEnvironment environment,
            KafkaConnectProperties properties,
            @Value("${embedded.kafka.containerBrokerList}") String kafkaContainerBrokerList,
            ObjectProvider<Network> network) {

        // https://kafka.apache.org/documentation/#connect_configuring
        // https://strimzi.io/blog/2021/03/29/connector-build/
        var connectProperties = "bootstrap.servers=%s\n".formatted(kafkaContainerBrokerList) +
                "group.id=strimzi-kafka-connect-cluster\n" +
                "config.storage.topic=strimzi-kafka-connect-cluster-configs\n" +
                "offset.storage.topic=strimzi-kafka-connect-cluster-offsets\n" +
                "status.storage.topic=strimzi-kafka-connect-cluster-status\n" +
                "key.converter=org.apache.kafka.connect.converters.ByteArrayConverter\n" +
                "key.converter.schemas.enable=false\n" +
                "value.converter=org.apache.kafka.connect.converters.ByteArrayConverter\n" +
                "value.converter.schemas.enable=false\n" +
                "config.storage.replication.factor=1\n" +
                "offset.storage.replication.factor=1\n" +
                "status.storage.replication.factor=1\n" +
                "offset.storage.file.filename=/tmp/connect.offsets\n" +
                "offset.flush.interval.ms=10000\n" +
                "plugin.path=/opt/kafka/plugins/camel\n";

        var kafkaConnect = new GenericContainer<>(
                new ImageFromDockerfile()
                        .withFileFromClasspath("Dockerfile", "kafka-connect-strimzi/Dockerfile"))
                .withCreateContainerCmdModifier(cmd -> cmd.withHostName(KAFKA_CONNECT_HOST_NAME))
//                .withEnv("REST_ADVERTISED_HOST_NAME", KAFKA_CONNECT_HOST_NAME)
                .withExposedPorts(properties.getPort())
                .withNetworkAliases(KAFKA_CONNECT_HOST_NAME)
                .withCopyToContainer(Transferable.of(connectProperties), "/opt/kafka/config/connect-standalone.properties");

        network.ifAvailable(kafkaConnect::withNetwork);

        kafkaConnect = configureCommonsAndStart(kafkaConnect, properties, log);
        registerSchemaRegistryEnvironment(kafkaConnect, environment, properties);

        return kafkaConnect;
    }

    private void registerSchemaRegistryEnvironment(
            GenericContainer<?> schemaRegistry,
                                                   ConfigurableEnvironment environment,
            KafkaConnectProperties properties) {

        var host = schemaRegistry.getHost();
        var port = schemaRegistry.getMappedPort(properties.getPort());

        var map = Map.<String, Object>ofEntries(
                entry("embedded.kafka-connect.host", host),
                entry("embedded.kafka-connect.port", port));

        log.info("Started Kafka Connect. Connection Details: {}, Connection URI: http://{}:{}", map, host, port);

        MapPropertySource propertySource = new MapPropertySource("embeddedSchemaRegistryInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
