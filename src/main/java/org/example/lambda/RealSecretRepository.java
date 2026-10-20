package org.example.lambda;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;

import java.util.Optional;

public class RealSecretRepository implements SecretRepository {

    private static final Logger log = LoggerFactory.getLogger(RealSecretRepository.class);

    // long-lived on purpose: reused across invocations so the connection pool survives
    private static final SecretsManagerClient SECRETS_MANAGER_CLIENT = buildClient();

    // read once per container; an absent value is not cached, so it's read again until it's set
    private String apiKey;

    private static SecretsManagerClient buildClient() {
        String region = System.getenv("APP_REGION");

        if (region == null || region.trim().isEmpty()) {
            throw new IllegalStateException("APP_REGION is null or empty");
        }

        log.info("creating SecretsManagerClient region={}", region);
        return SecretsManagerClient.builder().region(Region.of(region)).build();
    }

    @Override
    public Optional<String> getApiKey() {
        if (apiKey == null) {
            apiKey = readApiKey();
        }
        return Optional.ofNullable(apiKey);
    }

    private String readApiKey() {
        String secretName = System.getenv("SECRET_NAME");

        if (secretName == null) {
            throw new IllegalStateException("SECRET_NAME environment variable not set");
        }

        GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();

        try {
            return SECRETS_MANAGER_CLIENT.getSecretValue(getSecretValueRequest).secretString();

        } catch (ResourceNotFoundException e) {
            log.info("secret has no value secretName={}", secretName);
            return null;
        }
    }
}