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
        if (method.equals("GET")) {
            return handleGet();
        } else if (method.equals("POST")) {
            return handlePost(event);
        } else {
            log.warn("unsupported method: {}", method);
            return APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(405)
                    .withBody("Method Not Allowed")
                    .build();
        }
    }

    private APIGatewayV2HTTPResponse handleGet() {
        try {
            List<String> messages = helloWorld2.getAllMessagesOrdered();
            log.info("retrieved {} messages", messages.size());

            String body = "Stored messages: \n" + String.join("\n", messages);

            return APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(200)
                    .withBody(body)
                    .build();

        } catch (Exception e) {
            log.error("failed to retrieve messages", e);
            return APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(500)
                    .withBody("Error: " + e.getMessage())
                    .build();
        }
    }

    private APIGatewayV2HTTPResponse handlePost(APIGatewayV2HTTPEvent event) {
        try {
            String name = event.getQueryStringParameters() != null
                    ? event.getQueryStringParameters().get("name")
                    : null;

            String id = helloWorld2.handleRequest(name);
            log.info("stored name={} id={}", name, id);

            return APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(200)
                    .withBody("Stored: " + id)
                    .build();

        } catch (Exception e) {
            log.error("failed to store name", e);
            return APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(500)
                    .withBody("Error: " + e.getMessage())
                    .build();
        }
    }
}