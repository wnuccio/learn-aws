package org.example.lambda2;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HelloWorld2Test {

    @Test
    public void creates_message_and_stores_it_in_the_database() {
        TestContext context = new TestContext();

        String id = context.helloWorld2().handleRequest("John");

        assertEquals("Hello, John!", context.messageRepository.getMessage(id));
    }

    @Test
    public void retrieve_all_the_stored_messages() {
        TestContext context = new TestContext();

        context.helloWorld2().handleRequest("Walter");
        context.helloWorld2().handleRequest("John");

        List<String> messages = context.helloWorld2().getAllMessagesOrdered();

        assertEquals(2, messages.size(), "Message count should be 2 while it is " + messages.size());
        assertEquals("Hello, John!", messages.get(0));
        assertEquals("Hello, Walter!", messages.get(1));
    }
}