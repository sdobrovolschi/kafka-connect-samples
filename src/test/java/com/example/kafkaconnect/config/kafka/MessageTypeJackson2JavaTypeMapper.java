package com.example.kafkaconnect.config.kafka;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.kafka.common.header.Headers;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.kafka.support.mapping.Jackson2JavaTypeMapper;

import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class MessageTypeJackson2JavaTypeMapper implements Jackson2JavaTypeMapper {

    private final String messageType;
    private final Map<String, Class<?>> idClassMapping = new HashMap<>();
    private final Map<Class<?>, String> classIdMapping = new HashMap<>();

    public MessageTypeJackson2JavaTypeMapper(String messageType) {
        this.messageType = messageType;
    }

    public void setIdClassMapping(Map<String, Class<?>> idClassMapping) {
        this.idClassMapping.putAll(idClassMapping);
        createReverseMap();
    }

    private void createReverseMap() {
        classIdMapping.clear();
        for (var entry : idClassMapping.entrySet()) {
            String id = entry.getKey();
            Class<?> clazz = entry.getValue();
            classIdMapping.put(clazz, id);
        }
    }

    private JavaType getClassType(String type) {
        if (idClassMapping.containsKey(type)) {
            return TypeFactory.defaultInstance().constructType(idClassMapping.get(type));
        }
        throw new MessageConversionException("Failed to resolve class name. Class not found [" + type + "]");
    }

    @Override
    public void fromJavaType(JavaType javaType, Headers headers) {
        if (classIdMapping.containsKey(javaType.getRawClass())) {
            headers.add(messageType, classIdMapping.get(javaType.getRawClass()).getBytes(UTF_8));
        } else {
            throw new MessageConversionException("Failed to resolve type. Type not found [" + javaType.getRawClass() + "]");
        }
    }

    @Override
    public JavaType toJavaType(Headers headers) {
        if (headers.lastHeader(messageType) == null) {
            throw new IllegalArgumentException("Message type is required");
        }
        return getClassType(new String(headers.lastHeader(messageType).value(), UTF_8));
    }

    @Override
    public TypePrecedence getTypePrecedence() {
        return TypePrecedence.TYPE_ID;
    }

    @Override
    public void addTrustedPackages(String... packages) {

    }

    @Override
    public void fromClass(Class<?> clazz, Headers headers) {
        fromJavaType(TypeFactory.defaultInstance().constructType(clazz), headers);
    }

    @Override
    public Class<?> toClass(Headers headers) {
        return toJavaType(headers).getRawClass();
    }
}
