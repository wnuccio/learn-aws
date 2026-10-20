package org.example.lambda;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GetSecretTest {

    @Test
    public void returns_the_secret_value_masked() {
        TestContext context = new TestContext();
        context.secretRepository().setApiKey("my-api-key-123");

        Optional<String> result = context.getSecret().handleRequest();

        assertEquals(Optional.of("my-api********"), result);
    }

    @Test
    public void returns_nothing_when_the_secret_has_no_value() {
        TestContext context = new TestContext();
        context.secretRepository().setApiKey(null);

        Optional<String> result = context.getSecret().handleRequest();

        assertTrue(result.isEmpty());
    }
}