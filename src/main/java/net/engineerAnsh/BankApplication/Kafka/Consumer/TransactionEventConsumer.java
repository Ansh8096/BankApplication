package net.engineerAnsh.BankApplication.Kafka.Consumer;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Kafka.Event.TransactionEvent;
import net.engineerAnsh.BankApplication.Kafka.EventHandlers.TransactionEventHandler;
import net.engineerAnsh.BankApplication.Kafka.EventHandlers.TransactionEventHandlerRegistry;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionEventConsumer {

    private final TransactionEventHandlerRegistry registry;

    private String getEventType(TransactionEvent event) {
        JsonTypeName annotation = event.getClass().getAnnotation(JsonTypeName.class);  // It reads: @JsonTypeName("FRAUD_DETECTED")
        return annotation != null ? annotation.value() : "UNKNOWN";
    }

    @KafkaListener(
            topics = "transaction-events",
            groupId = "transaction-group"
    )
    @RetryableTopic(
            attempts = "1", // total attempts  (because of free tier we can't use more than 1 tier)
            backoff = @Backoff(delay = 5000), // 5 seconds delay
            dltTopicSuffix = ".dlt", // means:- transaction-events.dlt (it refers to <main-topic>.dlt)
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
    )
    public void consume(TransactionEvent event) {
        log.info("Successfully received an event: {}", getEventType(event));
        try {
            // It means: Given an event class -> give me the correct handler...
            TransactionEventHandler<TransactionEvent> handler =
                    registry.getHandler(event.getClass());

            if (handler == null) {
                log.warn("No handler found for event: {}", event.getClass().getSimpleName());
                return;
            }

            // It calls the correct implementation...
            handler.handle(event);

        } catch (Exception e) {
            log.error("Failed to process transaction event {}", getEventType(event), e);
            throw e;  // IMPORTANT: throw error to trigger retry ...
        }
    }

}