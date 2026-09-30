package org.example.lambda;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class RealS3Repository implements S3Repository {

    private static final Logger log = LoggerFactory.getLogger(RealS3Repository.class);

    // long-lived on purpose: reused across invocations so the connection pool survives
    private static final S3Client S3_CLIENT = buildClient();

    private static S3Client buildClient() {
        String region = System.getenv("APP_REGION");

        if (region == null || region.trim().isEmpty()) {
            throw new IllegalStateException("APP_REGION is null or empty");
        }

        log.info("creating S3Client region={}", region);
        return S3Client.builder().region(Region.of(region)).build();
    }

    @Override
    public void putObject(String key, S3Object object) {
        String bucketName = System.getenv("S3_BUCKET_NAME");

        if (bucketName == null) {
            throw new IllegalStateException("S3_BUCKET_NAME environment variable not set");
        }

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        S3_CLIENT.putObject(putObjectRequest, RequestBody.fromString(object.getMessage()));
    }

    @Override
    public S3Object getObject(String key) {
        // not implemented besides the fake
        return null;
    }
}
