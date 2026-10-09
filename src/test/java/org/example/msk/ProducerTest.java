package org.example.msk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ProducerTest {

    @Test
    public void sends_the_message_with_its_key() {
        TestContext context = new TestContext();

        context.producer().sendMessage("john", "hello");

        assertEquals(new Message("john", "hello"), context.messageProducer().getLastSentMessage());
    }

    @Test
    public void rejects_a_blank_message_without_sending_it() {
        TestContext context = new TestContext();

        assertThrows(IllegalArgumentException.class, () -> context.producer().sendMessage("john", "  "));

        assertNull(context.messageProducer().getLastSentMessage());
    }

    @Test
    public void rejects_a_null_key_without_sending_the_message() {
        TestContext context = new TestContext();

        assertThrows(IllegalArgumentException.class, () -> context.producer().sendMessage(null, "hello"));

        assertNull(context.messageProducer().getLastSentMessage());
    }
}