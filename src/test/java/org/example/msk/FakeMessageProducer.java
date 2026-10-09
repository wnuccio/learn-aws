package org.example.msk;

public class FakeMessageProducer implements MessageProducer {

    private Message lastSentMessage;

    @Override
    public void send(Message message) {
        lastSentMessage = message;
    }

    public Message getLastSentMessage() {
        return lastSentMessage;
    }
}