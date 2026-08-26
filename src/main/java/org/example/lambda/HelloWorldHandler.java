package org.example.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

@SuppressWarnings("unused")
public class HelloWorldHandler implements RequestHandler<String, String> {

    @Override
    public String handleRequest(String s, Context context) {
        context.getLogger().log("Received input: " + s);

        return new HelloWorld().handleRequest(s);
    }
}
