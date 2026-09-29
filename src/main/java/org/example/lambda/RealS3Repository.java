package org.example.lambda;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class RealS3Repository implements S3Repository {

    @Override
    public void putObject(String key, S3Object object) {
        String bucketName = System.getenv("S3_BUCKET_NAME");
        String region = System.getenv("AWS_REGION");

        if (bucketName == null) {
            throw new IllegalStateException("S3_BUCKET_NAME environment variable not set");
        }

        if (region == null) {
            throw new IllegalStateException("AWS_REGION environment variable not set");
        }

        try (S3Client s3Client = S3Client.builder().region(Region.of(region)).build()) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            RequestBody body = RequestBody.fromString(object.getMessage());
            s3Client.putObject(putObjectRequest, body);
        }
    }

    @Override
    public S3Object getObject(String key) {
        // TODO: implement real S3 read
        return null;
    }
}
