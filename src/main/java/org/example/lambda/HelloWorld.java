package org.example.lambda;

import java.time.Instant;

public class HelloWorld {
    private final S3Repository s3Repository;
    private final DynamoRepository dynamoRepository;

    public HelloWorld(S3Repository s3Repository, DynamoRepository dynamoRepository) {
        this.s3Repository = s3Repository;
        this.dynamoRepository = dynamoRepository;
    }

    public String handleRequest(String name) {
        String message = String.format("Hello, %s!", name);
        S3Object object = new S3Object(message);
        String key = (name != null ? name : "unknown") + "-" + Instant.now();
        s3Repository.putObject(key, object);
        dynamoRepository.putItem(key, message);
        return key;
    }
}
