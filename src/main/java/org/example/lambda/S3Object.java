package org.example.lambda;

import java.util.Objects;

public class S3Object {
    private final String message;

    public S3Object(String message) {
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        S3Object s3Object = (S3Object) o;
        return Objects.equals(message, s3Object.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message);
    }
}
