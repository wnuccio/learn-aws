package org.example.msk;

public record Message(String key, String value) {
    public Message {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Message key cannot be blank");
        }

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Message value cannot be blank");
        }
    }
}