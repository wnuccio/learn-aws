package org.example.msk;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class RealMessageProducer implements MessageProducer {

    private final String topic = System.getenv("TOPIC");
    private final KafkaProducer<String, String> producer;

    public RealMessageProducer() {
        Properties properties = KafkaConnection.properties(System.getenv("BOOTSTRAP_SERVERS"));
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producer = new KafkaProducer<>(properties);
    }

    @Override
    public void send(Message message) {
        try {
            // wait for the broker acknowledgement, so failures surface before the HTTP response
            producer.send(new ProducerRecord<>(topic, message.key(), message.value())).get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while sending to topic " + topic, e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("failed to send to topic " + topic, e.getCause());
        }
    }
}