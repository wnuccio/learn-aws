package org.example.msk;

public interface MessageProducer {
    void send(String key, String value);
}