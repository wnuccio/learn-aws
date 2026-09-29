package org.example.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;

@SuppressWarnings("unused")
public class HelloWorldHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        String name = event.getQueryStringParameters() != null
                ? event.getQueryStringParameters().get("name")
                : null;

        context.getLogger().log("Received name: " + name);

        // TODO: implement with real S3Repository

        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(200)
                .withBody("Not implemented yet")
                .build();
    }
}
