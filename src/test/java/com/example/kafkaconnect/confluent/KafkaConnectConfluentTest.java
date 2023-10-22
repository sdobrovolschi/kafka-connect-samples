package com.example.kafkaconnect.confluent;

import com.example.kafkaconnect.MessageReceiver;
import com.example.kafkaconnect.config.amqp.RabbitMQConfig;
import com.example.kafkaconnect.config.kafka.KafkaConfig;
import com.playtika.testcontainer.kafka.configuration.camel.EmbeddedKafkaCamelAutoConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitMessageOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.example.kafkaconnect.Money.TEN_EUROS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.messaging.support.MessageBuilder.withPayload;

// Kafka UI
// https://docs.kafka-ui.provectus.io/overview/readme
// https://www.kadeck.com/
// https://redpanda.com/redpanda-console-kafka-ui
// https://www.confluent.io/product/confluent-platform/gui-driven-management-and-monitoring/

@SpringBootApplication(exclude = EmbeddedKafkaCamelAutoConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import({RabbitMQConfig.class, KafkaConfig.class, MessageReceiver.class})
@ActiveProfiles({"test", "confluent"})
public class KafkaConnectConfluentTest {

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
//        https://docs.confluent.io/kafka-connectors/rabbitmq-source/current/config.html#rabbit-m-q-source-connector-config
                .bodyValue(new ClassPathResource("kafka-connect-confluent/create-connector-request.json"))
                .exchange()
                .expectStatus().isCreated();
    }

    //	https://docs.confluent.io/platform/current/connect/transforms/custom.html
    // https://docs.confluent.io/platform/current/connect/license.html
    @Test
    void streaming() {
        var message = withPayload(TEN_EUROS).build();

        rabbit.send("events", "", message);

        await("events").atMost(1, MINUTES).untilAsserted(
                () -> assertThat(messageReceiver.getEvents()).isNotEmpty()
        );
    }
}
