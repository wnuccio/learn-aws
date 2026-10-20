package org.example.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@SuppressWarnings("unused")
public class HelloWorldHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static final Logger log = LoggerFactory.getLogger(HelloWorldHandler.class);

    // built when Lambda constructs the handler, i.e. during the INIT phase
    private final HelloWorld helloWorld = new HelloWorld(new RealS3Repository(), new RealDynamoRepository());
    private final GetSecret getSecret = new GetSecret(new RealSecretRepository());

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        try {
            return switch (event.getRouteKey()) {
                case "GET /hello" -> handleHello(event);
                case "GET /secret" -> handleSecret();
                default -> throw new IllegalStateException("No handler for routeKey=" + event.getRouteKey());
            };

        } catch (Exception e) {
            log.error("failed to handle routeKey={}", event.getRouteKey(), e);
            return response(500, "Error: " + e.getMessage());
        }
    }

    private APIGatewayV2HTTPResponse handleHello(APIGatewayV2HTTPEvent event) {
        String name = event.getQueryStringParameters() != null
                ? event.getQueryStringParameters().get("name")
                : null;

        String key = helloWorld.handleRequest(name);
        log.info("stored name={} key={}", name, key);

        return response(200, "Stored: " + key);
    }

    private APIGatewayV2HTTPResponse handleSecret() {
        Optional<String> maskedApiKey = getSecret.handleRequest();

        return maskedApiKey
                .map(value -> response(200, "API key: " + value))
                .orElseGet(() -> response(404, "Secret has no value"));
    }

    private APIGatewayV2HTTPResponse response(int statusCode, String body) {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(statusCode)
                .withBody(body)
                .build();
    }
}