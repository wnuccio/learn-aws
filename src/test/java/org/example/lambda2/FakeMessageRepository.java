package org.example.lambda2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FakeMessageRepository implements MessageRepository {
    private final Map<String, String> messages = new HashMap<>();

    @Override
    public void insertMessage(String id, String message) {
        messages.put(id, message);
    }

    public String getMessage(String id) {
        return messages.get(id);
    }

    @Override
    public List<String> getAllMessagesOrdered() {
        return messages.values().stream()
                .sorted()
                .toList();
    }
}