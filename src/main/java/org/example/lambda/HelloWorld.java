package org.example.lambda;

public class HelloWorld {

    public HelloWorld() {
    }

    public String handleRequest(String s) {
        return String.format("Hello, %s!", s);
    }
}
