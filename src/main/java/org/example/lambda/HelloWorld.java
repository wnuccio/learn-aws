package org.example.lambda;

import java.time.Instant;

public class HelloWorld {
    private final S3Repository s3Repository;

    public HelloWorld(S3Repository s3Repository) {
        this.s3Repository = s3Repository;
    }

    public String handleRequest(String name) {
        String message = String.format("Hello, %s!", name);
        S3Object object = new S3Object(message);
        String key = (name != null ? name : "unknown") + "-" + Instant.now();
        s3Repository.putObject(key, object);
        return key;
    }
}
