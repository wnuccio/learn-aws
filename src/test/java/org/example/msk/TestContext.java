package org.example.msk;

public class TestContext {
    private final FakeMessageProducer messageProducer = new FakeMessageProducer();

    public FakeMessageProducer messageProducer() {
        return messageProducer;
    }

    public Producer producer() {
        return new Producer(messageProducer);
    }
}