package org.example.msk;

public class FakeMessageProducer implements MessageProducer {

    private Message lastSentMessage;

    @Override
    public void send(String key, String value) {
        lastSentMessage = new Message(key, value);
    }

    public Message getLastSentMessage() {
        return lastSentMessage;
    }
}