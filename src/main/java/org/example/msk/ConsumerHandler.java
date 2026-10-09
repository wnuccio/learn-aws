package org.example.msk;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.KafkaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@SuppressWarnings("unused")
public class ConsumerHandler implements RequestHandler<KafkaEvent, Void> {

    private static final Logger log = LoggerFactory.getLogger(ConsumerHandler.class);

    @Override
    public Void handleRequest(KafkaEvent event, Context context) {
        // records are grouped by "topic-partition"; Lambda delivers key and value base64-encoded
        event.getRecords().forEach((topicPartition, records) -> {
            for (KafkaEvent.KafkaEventRecord record : records) {
                log.info("received {} offset={} key={} value={}",
                        topicPartition, record.getOffset(), decode(record.getKey()), decode(record.getValue()));
            }
        });
        return null;
    }

    private String decode(String base64) {
        return base64 == null ? null : new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
    }
}