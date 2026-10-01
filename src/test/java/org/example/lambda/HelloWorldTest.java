package org.example.lambda;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HelloWorldTest {

    @Test
    public void creates_message_object_and_stores_it_in_S3() {
        TestContext context = new TestContext();

        String key = context.helloWorld().handleRequest("John");

        assertEquals(new S3Object("Hello, John!"), context.s3Repository.getObject(key));
    }

    @Test
    public void creates_message_object_and_stores_it_in_DynamoDB() {
        TestContext context = new TestContext();

        String key = context.helloWorld().handleRequest("John");

        assertEquals("Hello, John!", context.dynamoRepository.getItem(key));
    }
}
