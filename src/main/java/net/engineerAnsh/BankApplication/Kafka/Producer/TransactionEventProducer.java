package net.engineerAnsh.BankApplication.Kafka.Producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Kafka.Event.TransactionCompletedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionEventProducer {

    private static final String Topic = "transaction-completed";

    private final KafkaTemplate<String, TransactionCompletedEvent> kafkaTemplate;

    public void publishTransactionCompleted(TransactionCompletedEvent event){
        // Here, Key: transaction reference
        // Value: event object (auto-converted to JSON)
        kafkaTemplate.send(Topic,event.getTransactionReference(),event);

        log.info("Transaction event published: {}", event.getTransactionReference());
    }

}
