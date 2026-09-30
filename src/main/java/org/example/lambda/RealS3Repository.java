package org.example.lambda;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class RealS3Repository implements S3Repository {

    // long-lived on purpose: reused across invocations so the connection pool survives
    private static final S3Client S3_CLIENT = buildClient();

    private static S3Client buildClient() {
        String region = System.getenv("APP_REGION");

        if (region == null || region.trim().isEmpty()) {
            throw new IllegalStateException("APP_REGION is null or empty");
        }

        System.out.println("Creating S3Client with region: " + region);
        return S3Client.builder().region(Region.of(region)).build();
    }

    @Override
    public void putObject(String key, S3Object object) {
        System.out.println("putObject called with key: " + key);

        String bucketName = System.getenv("S3_BUCKET_NAME");

        System.out.println("Bucket: " + bucketName);

        if (bucketName == null) {
            throw new IllegalStateException("S3_BUCKET_NAME environment variable not set");
        }

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            System.out.println("Putting object...");
            RequestBody body = RequestBody.fromString(object.getMessage());
            S3_CLIENT.putObject(putObjectRequest, body);
            System.out.println("Object put successfully");

        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public S3Object getObject(String key) {
        // not implemented besides the fake
        return null;
    }
}
