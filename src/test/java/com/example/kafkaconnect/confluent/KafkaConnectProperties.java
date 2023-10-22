package com.example.kafkaconnect.confluent;

import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("embedded.kafka-connect")
@Getter
public class KafkaConnectProperties extends CommonContainerProperties {

    private int port = 8083;

    @Override
    public String getDefaultDockerImage() {
        return "confluentinc/cp-kafka-connect-base:7.5.1";
    }
}
