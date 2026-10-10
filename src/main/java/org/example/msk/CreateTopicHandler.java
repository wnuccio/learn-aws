package org.example.msk;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.Map;

// invoked once by Terraform (aws_lambda_invocation) during `terraform apply`, so the topic exists before the event source mapping
@SuppressWarnings("unused")
public class CreateTopicHandler implements RequestHandler<Map<String, Object>, String> {

    @Override
    public String handleRequest(Map<String, Object> event, Context context) {
        String topic = System.getenv("TOPIC");
        String bootstrapServers = System.getenv("BOOTSTRAP_SERVERS");

        // an exception makes the invocation fail, which in turn fails the apply
        new TopicCreator(bootstrapServers, topic).createIfMissing();

        return "topic " + topic + " created";
    }
}