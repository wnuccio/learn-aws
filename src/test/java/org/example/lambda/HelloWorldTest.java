package org.example.lambda;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HelloWorldTest {

    @Test
    public void creates_message_object_and_stores_it_in_S3() {
        FakeS3Repository s3Repository = new FakeS3Repository();
        FakeDynamoRepository dynamoRepository = new FakeDynamoRepository();
        HelloWorld helloWorld = new HelloWorld(s3Repository, dynamoRepository);

        String key = helloWorld.handleRequest("John");

        assertEquals(new S3Object("Hello, John!"), s3Repository.getObject(key));
    }

    @Test
    public void creates_message_object_and_stores_it_in_DynamoDB() {
        FakeS3Repository s3Repository = new FakeS3Repository();
        FakeDynamoRepository dynamoRepository = new FakeDynamoRepository();
        HelloWorld helloWorld = new HelloWorld(s3Repository, dynamoRepository);

        String key = helloWorld.handleRequest("John");

        assertEquals("Hello, John!", dynamoRepository.getItem(key));
    }
}
