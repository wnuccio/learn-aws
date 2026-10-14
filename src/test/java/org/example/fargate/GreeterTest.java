package org.example.fargate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GreeterTest {

    @Test
    public void greets_by_name() {
        Greeter greeter = new Greeter();

        String greeting = greeter.greet("John");

        assertEquals("Hello, John!", greeting);
    }
}