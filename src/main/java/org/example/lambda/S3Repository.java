package org.example.lambda;

public interface S3Repository {
    void putObject(String key, S3Object object);
}