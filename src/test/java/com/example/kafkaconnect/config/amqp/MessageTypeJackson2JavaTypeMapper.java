package com.example.kafkaconnect.config.amqp;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.MessageConversionException;

import java.util.HashMap;
import java.util.Map;

public class MessageTypeJackson2JavaTypeMapper implements Jackson2JavaTypeMapper {

    private final Map<String, Class<?>> idClassMapping = new HashMap<>();
    private final Map<Class<?>, String> classIdMapping = new HashMap<>();

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

    @Override
    public void fromJavaType(JavaType javaType, MessageProperties properties) {
        if (classIdMapping.containsKey(javaType.getRawClass())) {
            properties.setType(classIdMapping.get(javaType.getRawClass()));
        } else {
            throw new MessageConversionException("Failed to resolve type. Type not found [" + javaType.getRawClass() + "]");
        }
    }

    @Override
    public JavaType toJavaType(MessageProperties properties) {
        if (properties.getType() == null) {
            throw new IllegalArgumentException("Message type is required");
        }
        return getClassType(properties.getType());
    }

    private JavaType getClassType(String type) {
        if (idClassMapping.containsKey(type)) {
            return TypeFactory.defaultInstance().constructType(idClassMapping.get(type));
        }
        throw new MessageConversionException("Failed to resolve class name. Class not found [" + type + "]");
    }

    @Override
    public TypePrecedence getTypePrecedence() {
        return TypePrecedence.TYPE_ID;
    }

    @Override
    public JavaType getInferredType(MessageProperties properties) {
        return null;
    }

    @Override
    public void fromClass(Class<?> clazz, MessageProperties properties) {
        fromJavaType(TypeFactory.defaultInstance().constructType(clazz), properties);
    }

    @Override
    public Class<?> toClass(MessageProperties properties) {
        return toJavaType(properties).getRawClass();
    }
}
