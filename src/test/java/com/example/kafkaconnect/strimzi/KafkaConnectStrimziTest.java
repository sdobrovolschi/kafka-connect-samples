package com.example.kafkaconnect.strimzi;

import com.example.kafkaconnect.MessageReceiver;
import com.example.kafkaconnect.config.amqp.RabbitMQConfig;
import com.example.kafkaconnect.config.kafka.KafkaConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitMessageOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.example.kafkaconnect.Money.TEN_EUROS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.kafka.config.TopicBuilder.name;
import static org.springframework.messaging.support.MessageBuilder.withPayload;

@SpringBootApplication
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import({RabbitMQConfig.class, KafkaConfig.class, MessageReceiver.class})
@ActiveProfiles({"test", "strimzi"})
class KafkaConnectStrimziTest {

    @Autowired
    RabbitMessageOperations rabbit;

    @Autowired
    MessageReceiver messageReceiver;

    @BeforeAll
    static void createConnector(
            @Autowired WebTestClient client,
            @Value("${embedded.kafka-connect.host}") String host,
            @Value("${embedded.kafka-connect.port}") String port) {

        client
                .post().uri("http://%s:%s/connectors".formatted(host, port))
                .contentType(APPLICATION_JSON)
                // https://camel.apache.org/camel-kafka-connector/3.20.x/reference/connectors/camel-rabbitmq-source-kafka-source-connector.html
                // org.apache.camel.component.rabbitmq.RabbitMQEndpoint
                .bodyValue(new ClassPathResource("kafka-connect-strimzi/create-connector-request.json"))
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void streaming() {
        var message = withPayload(TEN_EUROS).build();

        rabbit.send("events", "", message);


        await("events").atMost(1, MINUTES).untilAsserted(
                () -> assertThat(messageReceiver.getEvents()).isNotEmpty()
        );
    }

    @TestConfiguration
    static class Config {

        @Bean
        NewTopic eventsTopic() {
            return name("events").build();
        }
    }
}
