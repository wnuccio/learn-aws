package org.example.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unused")
public class HelloWorldHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static final Logger log = LoggerFactory.getLogger(HelloWorldHandler.class);

    // built when Lambda constructs the handler, i.e. during the INIT phase
    private final HelloWorld helloWorld = new HelloWorld(new RealS3Repository(), new RealDynamoRepository());

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        try {
            String name = event.getQueryStringParameters() != null
                    ? event.getQueryStringParameters().get("name")
                    : null;

            String key = helloWorld.handleRequest(name);
            log.info("stored name={} key={}", name, key);

            return APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(200)
                    .withBody("Stored: " + key)
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
