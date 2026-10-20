package org.example.lambda2;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@SuppressWarnings("unused")
public class HelloWorld2Handler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static final Logger log = LoggerFactory.getLogger(HelloWorld2Handler.class);

    // built when Lambda constructs the handler, i.e. during the INIT phase
    private final HelloWorld2 helloWorld2 = new HelloWorld2(new RealMessageRepository());

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        String method = event.getRequestContext().getHttp().getMethod();
        try {
            return switch (method) {
                case "GET" -> handleGet();
                case "POST" -> handlePost(event);
                default -> handleUnsupported(method);
            };

        } catch (Exception e) {
            log.error("failed to handle method={}", method, e);
            return response(500, "Error: " + e.getMessage());
        }
    }

    private APIGatewayV2HTTPResponse handleGet() {
        List<String> messages = helloWorld2.getAllMessagesOrdered();
        log.info("retrieved {} messages", messages.size());

        return response(200, "Stored messages: \n" + String.join("\n", messages));
    }

    private APIGatewayV2HTTPResponse handlePost(APIGatewayV2HTTPEvent event) {
        String name = event.getQueryStringParameters() != null
                ? event.getQueryStringParameters().get("name")
                : null;

        String id = helloWorld2.handleRequest(name);
        log.info("stored name={} id={}", name, id);

        return response(200, "Stored: " + id);
    }

    private APIGatewayV2HTTPResponse handleUnsupported(String method) {
        log.warn("unsupported method: {}", method);
        return response(405, "Method Not Allowed");
    }

    private APIGatewayV2HTTPResponse response(int statusCode, String body) {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(statusCode)
                .withBody(body)
                .build();
    }
}