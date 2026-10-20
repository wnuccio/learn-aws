package org.example.lambda;

public class TestContext {
    public final FakeS3Repository s3Repository = new FakeS3Repository();
    public final FakeDynamoRepository dynamoRepository = new FakeDynamoRepository();
    private final FakeSecretRepository secretRepository = new FakeSecretRepository();

    public HelloWorld helloWorld() {
        return new HelloWorld(s3Repository, dynamoRepository);
    }

    public FakeSecretRepository secretRepository() {
        return secretRepository;
    }

    public GetSecret getSecret() {
        return new GetSecret(secretRepository);
    }
}