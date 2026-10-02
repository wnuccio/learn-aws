package org.example.lambda2;

public class TestContext {
    public final FakeMessageRepository messageRepository = new FakeMessageRepository();

    public HelloWorld2 helloWorld2() {
        return new HelloWorld2(messageRepository);
    }
}