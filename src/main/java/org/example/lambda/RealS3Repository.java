package org.example.lambda;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class RealS3Repository implements S3Repository {

    @Override
    public void putObject(String key, S3Object object) {
        System.out.println("putObject called with key: " + key);

        String bucketName = System.getenv("S3_BUCKET_NAME");
        String region = System.getenv("APP_REGION");

        System.out.println("Bucket: " + bucketName + ", Region: " + region);

        if (bucketName == null) {
            throw new IllegalStateException("S3_BUCKET_NAME environment variable not set");
        }

        if (region == null) {
            throw new IllegalStateException("APP_REGION environment variable not set");
        }

        System.out.println("Creating S3Client with region: " + region);
        if (region == null || region.trim().isEmpty()) {
            throw new IllegalStateException("APP_REGION is null or empty");
        }

        try (S3Client s3Client = S3Client.builder().region(Region.of(region)).build()) {
            System.out.println("S3Client created, building request...");

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            System.out.println("Putting object...");
            RequestBody body = RequestBody.fromString(object.getMessage());
            s3Client.putObject(putObjectRequest, body);
            System.out.println("Object put successfully");

        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public S3Object getObject(String key) {
        // TODO: implement real S3 read
        return null;
    }
}
