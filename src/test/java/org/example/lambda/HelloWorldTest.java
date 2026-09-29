package org.example.lambda;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HelloWorldTest {

    @Test
    public void creates_messag_object_retrievable_by_key() {
        FakeS3Repository s3Repository = new FakeS3Repository();
        HelloWorld helloWorld = new HelloWorld(s3Repository);

        String key = helloWorld.handleRequest("John");

        assertEquals(new S3Object("Hello, John!"), s3Repository.getObject(key));
    }
}