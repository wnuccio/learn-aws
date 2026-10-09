package org.example.msk;

public class Producer {
    private final MessageProducer messageProducer;

    public Producer(MessageProducer messageProducer) {
        this.messageProducer = messageProducer;
    }

    public void sendMessage(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Message key cannot be blank");
        }

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Message value cannot be blank");
        }

        messageProducer.send(key, value);
    }
}