package net.engineerAnsh.BankApplication.Kafka.Producer;

import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Kafka.Event.TransactionSuccessEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransactionSuccessProducer {

    private static final String SUCCESS_TOPIC = "transaction.completed.success";

    private final KafkaTemplate<String, TransactionSuccessEvent> kafkaTemplate;

    public void publishSuccess(String transactionReference){
        TransactionSuccessEvent transactionSuccessEvent = new TransactionSuccessEvent(
                transactionReference,
                LocalDateTime.now()
        );
        kafkaTemplate.send(SUCCESS_TOPIC ,transactionReference,transactionSuccessEvent);
    }
}
