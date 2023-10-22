package com.example.kafkaconnect;

import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.Network;

class TestcontainersConfig {

    @Bean
    Network network() {
        return Network.newNetwork();
    }
}
