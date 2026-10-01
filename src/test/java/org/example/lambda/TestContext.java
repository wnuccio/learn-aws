package org.example.lambda;

public class TestContext {
    public final FakeS3Repository s3Repository = new FakeS3Repository();
    public final FakeDynamoRepository dynamoRepository = new FakeDynamoRepository();

    public HelloWorld helloWorld() {
        return new HelloWorld(s3Repository, dynamoRepository);
    }
}