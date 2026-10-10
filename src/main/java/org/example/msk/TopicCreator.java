package org.example.msk;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.errors.TopicExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

// topics can't be created by Terraform on MSK Serverless, nor auto-created by the broker
public class TopicCreator {

    private static final Logger log = LoggerFactory.getLogger(TopicCreator.class);

    private static final int PARTITIONS = 2;

    private final String bootstrapServers;
    private final String topic;

    public TopicCreator(String bootstrapServers, String topic) {
        this.bootstrapServers = bootstrapServers;
        this.topic = topic;
    }

    public void createIfMissing() {
        try (AdminClient admin = AdminClient.create(KafkaConnection.properties(bootstrapServers))) {
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
}