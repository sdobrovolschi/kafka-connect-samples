package com.example.kafkaconnect;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Collections.unmodifiableList;

@Component
@Slf4j
public class MessageReceiver {

    private final List<Message<Money>> events = new CopyOnWriteArrayList<>();

    @KafkaListener(topics = "events", groupId = "test")
    void on(Message<Money> event) {
        log.info("event {}", event);
        events.add(event);
    }

    public List<Message<Money>> getEvents() {
        return unmodifiableList(events);
    }
}
