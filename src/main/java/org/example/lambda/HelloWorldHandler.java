package org.example.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;

@SuppressWarnings("unused")
public class HelloWorldHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        try {
            String name = event.getQueryStringParameters() != null
                    ? event.getQueryStringParameters().get("name")
                    : null;

            context.getLogger().log("Received name: " + name);

            context.getLogger().log("Creating S3Repository...");
            S3Repository s3Repository = new RealS3Repository();
            context.getLogger().log("Creating HelloWorld...");
            HelloWorld helloWorld = new HelloWorld(s3Repository);

            context.getLogger().log("Calling handleRequest...");
            String key = helloWorld.handleRequest(name);
            context.getLogger().log("Stored in S3 with key: " + key);

            context.getLogger().log("Building response...");
            APIGatewayV2HTTPResponse response = APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(200)
                    .withBody("Stored: " + key)
                    .build();

            context.getLogger().log("Response built successfully");
            return response;

        } catch (Exception e) {
            context.getLogger().log("ERROR: " + e.getMessage());
            e.printStackTrace();
            return APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(500)
                    .withBody("Error: " + e.getMessage())
                    .build();

        } finally {
            context.getLogger().log("Handler execution completed");
        }
    }
}
