package org.example.lambda2;

import java.util.List;

public interface MessageRepository {
    void insertMessage(String id, String message);

    List<String> getAllMessagesOrdered();
}