package org.example.msk;

public class Producer {
    private final MessageProducer messageProducer;

    public Producer(MessageProducer messageProducer) {
        this.messageProducer = messageProducer;
    }

    public void sendMessage(String key, String value) {
        messageProducer.send(new Message(key, value));
    }
}