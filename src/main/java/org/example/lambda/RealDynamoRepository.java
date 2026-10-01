package org.example.lambda;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.Map;

public class RealDynamoRepository implements DynamoRepository {

    private static final Logger log = LoggerFactory.getLogger(RealDynamoRepository.class);

    // long-lived on purpose: reused across invocations so the connection pool survives
    private static final DynamoDbClient DYNAMODB_CLIENT = buildClient();

    private static DynamoDbClient buildClient() {
        String region = System.getenv("APP_REGION");

        if (region == null || region.trim().isEmpty()) {
            throw new IllegalStateException("APP_REGION is null or empty");
        }

        log.info("creating DynamoDbClient region={}", region);
        return DynamoDbClient.builder().region(Region.of(region)).build();
    }

    @Override
    public void putItem(String id, String message) {
        String tableName = System.getenv("DYNAMODB_TABLE_NAME");

        if (tableName == null) {
            throw new IllegalStateException("DYNAMODB_TABLE_NAME environment variable not set");
        }

        PutItemRequest putItemRequest = PutItemRequest.builder()
                .tableName(tableName)
                .item(Map.of(
                        "id", AttributeValue.builder().s(id).build(),
                        "message", AttributeValue.builder().s(message).build()
                ))
                .build();

        DYNAMODB_CLIENT.putItem(putItemRequest);
    }
}