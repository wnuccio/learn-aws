package org.example.msk;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class RealMessageProducer implements MessageProducer {

    private static final Logger log = LoggerFactory.getLogger(RealMessageProducer.class);

    private static final int PARTITIONS = 2;

    private final String topic = System.getenv("TOPIC");
    private final KafkaProducer<String, String> producer;

    public RealMessageProducer() {
        String bootstrapServers = System.getenv("BOOTSTRAP_SERVERS");

        createTopicIfMissing(bootstrapServers);

        Properties properties = connectionProperties(bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producer = new KafkaProducer<>(properties);
    }

    @Override
    public void send(String key, String value) {
        try {
            // wait for the broker acknowledgement, so failures surface before the HTTP response
            producer.send(new ProducerRecord<>(topic, key, value)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while sending to topic " + topic, e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("failed to send to topic " + topic, e.getCause());
        }
    }

    // topics can't be created by Terraform on MSK Serverless, nor auto-created by the broker
    private void createTopicIfMissing(String bootstrapServers) {
        try (AdminClient admin = AdminClient.create(connectionProperties(bootstrapServers))) {
            // replication factor is managed by MSK Serverless, so leave it empty to use the broker default
            NewTopic newTopic = new NewTopic(topic, Optional.of(PARTITIONS), Optional.empty());
            admin.createTopics(List.of(newTopic)).all().get();
            log.info("created topic {}", topic);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while creating topic " + topic, e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof TopicExistsException) {
                log.info("topic {} already exists", topic);
            } else {
                throw new IllegalStateException("failed to create topic " + topic, e.getCause());
            }
        }
    }

    private Properties connectionProperties(String bootstrapServers) {
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