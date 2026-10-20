package org.example.lambda;

import java.util.Optional;

public class GetSecret {
    private final SecretRepository secretRepository;

    public GetSecret(SecretRepository secretRepository) {
        this.secretRepository = secretRepository;
    }

    public Optional<String> handleRequest() {
        return secretRepository.getApiKey().map(this::mask);
    }

    private String mask(String apiKey) {
        final int maskLength = 6;
        return apiKey.substring(0, maskLength) + "*".repeat(apiKey.length() - maskLength);
    }
}