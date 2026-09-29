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

        S3Repository s3Repository = new RealS3Repository();
        HelloWorld helloWorld = new HelloWorld(s3Repository);
        String key = helloWorld.handleRequest(name);

        context.getLogger().log("Stored in S3 with key: " + key);

        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(200)
                .withBody( String.format("Object for name: '%s' stored with key: '%s'", name, key))
                .build();
    }
}
