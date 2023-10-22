package com.example.kafkaconnect.config.amqp;

import com.example.kafkaconnect.Money;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.Map;

import static java.util.Map.entry;
import static org.springframework.amqp.support.converter.Jackson2JavaTypeMapper.TypePrecedence.TYPE_ID;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@TestConfiguration
public class RabbitMQConfig {

    @Bean
    MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        var converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setAssumeSupportedContentType(false);
        converter.setTypePrecedence(TYPE_ID);
        converter.setNullAsOptionalEmpty(true);
        converter.setSupportedContentType(APPLICATION_JSON);
        converter.setJavaTypeMapper(typeMapper());

        return converter;
    }

    Jackson2JavaTypeMapper typeMapper() {
        var mapper = new MessageTypeJackson2JavaTypeMapper();
        mapper.setIdClassMapping(Map.ofEntries(
                entry("money", Money.class)));

        return mapper;
    }
}
