package org.example.lambda;

import java.util.HashMap;
import java.util.Map;

public class FakeS3Repository implements S3Repository {
    private final Map<String, S3Object> objects = new HashMap<>();

    @Override
    public void putObject(String key, S3Object object) {
        objects.put(key, object);
    }

    @Override
    public S3Object getObject(String key) {
        return objects.get(key);
    }
}