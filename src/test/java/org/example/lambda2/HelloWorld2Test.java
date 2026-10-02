package org.example.lambda2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HelloWorld2Test {

    @Test
    public void creates_message_and_stores_it_in_the_database() {
        TestContext context = new TestContext();

        String id = context.helloWorld2().handleRequest("John");

        assertEquals("Hello, John!", context.messageRepository.getMessage(id));
    }
}