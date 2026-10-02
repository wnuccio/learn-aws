package org.example.lambda2;

import java.time.Instant;

public class HelloWorld2 {
    private final MessageRepository messageRepository;

    public HelloWorld2(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public String handleRequest(String name) {
        String message = String.format("Hello, %s!", name);
        String id = (name != null ? name : "unknown") + "-" + Instant.now();
        messageRepository.insertMessage(id, message);
        return id;
    }
}