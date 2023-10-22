package com.example.kafkaconnect.confluent;

import com.playtika.testcontainer.kafka.configuration.EmbeddedKafkaBootstrapConfiguration;
import lombok.extern.slf4j.Slf4j;
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

import java.util.Map;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static java.util.Map.entry;

@Configuration
@AutoConfigureOrder
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(EmbeddedKafkaBootstrapConfiguration.class)
@EnableConfigurationProperties(KafkaConnectProperties.class)
@Slf4j
public class EmbeddedConfluentKafkaConnectBootstrapConfig {

    public static final String KAFKA_CONNECT_HOST_NAME = "kafka-connect.testcontainer.docker";

    @Bean(name = "kafka-connect-confluent", destroyMethod = "stop")
    public GenericContainer<?> kafkaConnect(
            ConfigurableEnvironment environment,
            KafkaConnectProperties properties,
            @Value("${embedded.kafka.containerBrokerList}") String kafkaContainerBrokerList,
            Network network) {

        GenericContainer<?> kafkaConnect = new GenericContainer<>(
                new ImageFromDockerfile()
                        .withFileFromClasspath("Dockerfile", "kafka-connect-confluent/Dockerfile"))
                .withCreateContainerCmdModifier(cmd -> cmd.withHostName(KAFKA_CONNECT_HOST_NAME))
                // https://docs.confluent.io/platform/current/installation/docker/config-reference.html
                .withEnv("CONNECT_BOOTSTRAP_SERVERS", kafkaContainerBrokerList)
//                .withEnv("CONNECT_HOST", KAFKA_CONNECT_HOST_NAME)
                .withEnv("CONNECT_GROUP_ID", "kafka-connect-confluent")
                .withEnv("CONNECT_CONFIG_STORAGE_TOPIC", "kafka-connect-config")
                .withEnv("CONNECT_OFFSET_STORAGE_TOPIC", "kafka-connect-offset")
                .withEnv("CONNECT_STATUS_STORAGE_TOPIC", "kafka-connect-status")
                // https://docs.confluent.io/platform/current/connect/index.html#converters
                // https://kafka.apache.org/documentation/#connect_configuring
                .withEnv("CONNECT_KEY_CONVERTER", "org.apache.kafka.connect.converters.ByteArrayConverter")
                .withEnv("CONNECT_KEY_CONVERTER_SCHEMAS_ENABLE", "false")
                .withEnv("CONNECT_VALUE_CONVERTER", "org.apache.kafka.connect.converters.ByteArrayConverter")
                .withEnv("CONNECT_VALUE_CONVERTER_SCHEMAS_ENABLE", "false")
                .withEnv("CONNECT_REST_ADVERTISED_HOST_NAME", KAFKA_CONNECT_HOST_NAME)
                .withEnv("CONNECT_CONFIG_STORAGE_REPLICATION_FACTOR", "1")
                .withEnv("CONNECT_OFFSET_STORAGE_REPLICATION_FACTOR", "1")
                .withEnv("CONNECT_STATUS_STORAGE_REPLICATION_FACTOR", "1")
                .withExposedPorts(properties.getPort())
                .withNetwork(network)
                .withNetworkAliases(KAFKA_CONNECT_HOST_NAME);


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
