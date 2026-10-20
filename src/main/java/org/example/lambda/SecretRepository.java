package org.example.lambda;

import java.util.Optional;

public interface SecretRepository {
    Optional<String> getApiKey();
}