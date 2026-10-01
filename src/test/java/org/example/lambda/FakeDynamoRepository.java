package org.example.lambda;

import java.util.HashMap;
import java.util.Map;

public class FakeDynamoRepository implements DynamoRepository {
    private final Map<String, String> items = new HashMap<>();

    @Override
    public void putItem(String id, String message) {
        items.put(id, message);
    }

    public String getItem(String key) {
        return items.get(key);
    }
}