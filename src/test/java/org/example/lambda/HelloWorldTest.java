package org.example.lambda;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HelloWorldTest {

    @Test
    public void returnsHelloWorldMessage() {
        HelloWorld helloWorld = new HelloWorld();

        String result = helloWorld.handleRequest("World");

        assertEquals("Hello, World!", result);
    }

    @Test
    public void returnsHelloLambdaMessage() {
        HelloWorld helloWorld = new HelloWorld();

        String result = helloWorld.handleRequest("Lambda");

        assertEquals("Hello, Lambda!", result);
    }

}