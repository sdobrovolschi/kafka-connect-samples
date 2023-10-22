package com.example.kafkaconnect.strimzi;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.kafka.properties.KafkaConfigurationProperties;
import io.strimzi.test.container.StrimziKafkaContainer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
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

import java.util.Map;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static io.strimzi.test.container.StrimziKafkaContainer.KAFKA_PORT;
import static java.util.Map.entry;

@Configuration
@AutoConfigureOrder
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@EnableConfigurationProperties(KafkaConfigurationProperties.class)
@Slf4j
public class EmbeddedStrimziKafkaBootstrapConfig {

    private static final String KAFKA_NETWORK_ALIAS = "kafka-broker.testcontainer.docker";

    @Bean(destroyMethod = "stop")
    public GenericContainer<?> kafka(
            ConfigurableEnvironment environment,
            ObjectProvider<Network> network,
            KafkaConfigurationProperties properties) {

        //		https://github.com/strimzi/test-container
        var kafka = new StrimziKafkaContainer("quay.io/strimzi/kafka:0.38.0-kafka-3.6.0")
//                .withExposedPorts(properties.getBrokerPort(), properties.getHttpPort())
                .withNetworkAliases(KAFKA_NETWORK_ALIAS)
                .waitForRunning();

        network.ifAvailable(kafka::withNetwork);

        kafka = (StrimziKafkaContainer) configureCommonsAndStart(kafka, properties, log);
        registerKafkaEnvironment(kafka, environment);
        return kafka;
    }

    private void registerKafkaEnvironment(StrimziKafkaContainer kafka, ConfigurableEnvironment environment) {
        var map = Map.<String, Object>ofEntries(
                entry("embedded.kafka.brokerList", "%s:%d".formatted(kafka.getHost(), kafka.getMappedPort(KAFKA_PORT))),
                entry("embedded.kafka.containerBrokerList", "%s:%d".formatted(KAFKA_NETWORK_ALIAS, 9093))
        );

        var propertySource = new MapPropertySource("embeddedKafkaInfo", map);

        log.info("Started kafka broker. Connection details: {}", map);

        environment.getPropertySources().addFirst(propertySource);
    }
}
