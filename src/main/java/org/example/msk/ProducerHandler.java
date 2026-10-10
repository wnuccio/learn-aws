package org.example.msk;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unused")
public class ProducerHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static final Logger log = LoggerFactory.getLogger(ProducerHandler.class);

    // built when Lambda constructs the handler, i.e. during the INIT phase
    private final Producer producer = new Producer(new RealMessageProducer());

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        String method = event.getRequestContext().getHttp().getMethod();
        if (!method.equals("POST")) {
            log.warn("unsupported method: {}", method);
            return response(405, "Method Not Allowed");
        }

        try {
            String key = event.getQueryStringParameters() != null
                    ? event.getQueryStringParameters().get("key")
                    : null;

            producer.sendMessage(key, event.getBody());
            log.info("sent message with key={}", key);

            return response(200, "Sent");

        } catch (IllegalArgumentException e) {
            log.warn("invalid request: {}", e.getMessage());
            return response(400, "Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("failed to send message", e);
            return response(500, "Error: " + e.getMessage());
        }
    }

    private APIGatewayV2HTTPResponse response(int statusCode, String body) {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(statusCode)
                .withBody(body)
                .build();
    }
}