package org.example.lambda;

public interface DynamoRepository {
    void putItem(String id, String message);
}