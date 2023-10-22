package com.example.kafkaconnect.config.kafka;

import com.example.kafkaconnect.Money;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.mapping.Jackson2JavaTypeMapper;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.Map;

import static java.util.Map.entry;

@TestConfiguration
public class KafkaConfig {

    //		@Bean
//		DefaultKafkaConsumerFactoryCustomizer customizer(ObjectMapper objectMapper) {
//			return customizer -> {
//				customizer.setKeyDeserializer(new ErrorHandlingDeserializer<String>(new StringDeserializer()));
//				customizer.setValueDeserializer(new ErrorHandlingDeserializer<Object>(new JsonDeserializer<>(objectMapper)));
//			};
//		}

    @Bean
    ConsumerFactory<String, ?> kafkaConsumerFactory(
            KafkaProperties properties,
            ObjectMapper objectMapper,
            ObjectProvider<DefaultKafkaConsumerFactoryCustomizer> customizers) {

        var configs = properties.buildConsumerProperties();

        var factory = new DefaultKafkaConsumerFactory<>(configs,
                new ErrorHandlingDeserializer<>(new StringDeserializer()),
                new ErrorHandlingDeserializer<>(new JsonDeserializer<>(objectMapper).typeMapper(kafkaTypeMapper())));

        customizers.orderedStream().forEach(customizer -> customizer.customize(factory));

        return factory;
    }

    Jackson2JavaTypeMapper kafkaTypeMapper() {
        var mapper = new MessageTypeJackson2JavaTypeMapper("rabbitmq.type");
//        var mapper = new MessageTypeJackson2JavaTypeMapper("CamelHeader.CamelRabbitmqType");

        mapper.setIdClassMapping(Map.ofEntries(
                entry("money", Money.class)));

        return mapper;
    }
}
