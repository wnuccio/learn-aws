package org.example.msk;

import java.util.Map;
import java.util.Properties;

// connection settings shared by every Kafka client talking to MSK with IAM authentication
class KafkaConnection {

    static Properties properties(String bootstrapServers) {
        Properties properties = new Properties();
        properties.putAll(Map.of(
                "bootstrap.servers", bootstrapServers,
                "security.protocol", "SASL_SSL",
                "sasl.mechanism", "AWS_MSK_IAM",
                "sasl.jaas.config", "software.amazon.msk.auth.iam.IAMLoginModule required;",
                "sasl.client.callback.handler.class", "software.amazon.msk.auth.iam.IAMClientCallbackHandler"
        ));
        return properties;
    }
}