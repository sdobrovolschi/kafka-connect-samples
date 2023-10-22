package com.example.kafkaconnect.strimzi;

import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("embedded.kafka-connect")
@Getter
public class KafkaConnectProperties extends CommonContainerProperties {

    private int port = 8083;

    @Override
    public String getDefaultDockerImage() {
        return "quay.io/strimzi/kafka:0.38.0-kafka-3.6.0";
    }
}
