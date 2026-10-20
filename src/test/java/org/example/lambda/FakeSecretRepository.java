package org.example.lambda;

import java.util.Optional;

public class FakeSecretRepository implements SecretRepository {
    private String apiKey;

    @Override
    public Optional<String> getApiKey() {
        return Optional.ofNullable(apiKey);
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}