package net.engineerAnsh.BankApplication.Kafka.Consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Email.EmailServiceImpl;
import net.engineerAnsh.BankApplication.Kafka.Event.TransactionCompletedEvent;
import net.engineerAnsh.BankApplication.Kafka.Producer.TransactionSuccessProducer;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionNotificationConsumer {

    private final EmailServiceImpl emailService;
    private final TransactionSuccessProducer transactionSuccessProducer;

    @RetryableTopic(
            attempts = "3", // total attempts
            backoff = @Backoff(delay = 5000), // 5 seconds delay
            dltTopicSuffix = ".dlt", // means:- transaction.completed.dlt (it refers to <main-topic>.dlt)
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
    )
    @KafkaListener(
            topics = "transaction.completed",
            groupId = "transaction-notification-group"
    )
    public void consume(TransactionCompletedEvent event) {
        log.info("Received transaction event: {}", event.getTransactionReference());

        try {

            String subject = "Transaction Alert - " + event.getType();

            String body = """
                    Hello,
                    
                    A transaction has been completed on your account.
                    
                    Reference: %s
                    Type: %s
                    Amount: ₹%s
                    From: %s
                    To: %s
                    Time: %s
                    Remark: %s
                    
                    Thank you,
                    BANK OF ANSH
                    """.formatted(
                    event.getTransactionReference(),
                    event.getType(),
                    event.getAmount(),
                    event.getFromAccountMasked(),
                    event.getToAccountMasked(),
                    event.getCreatedAt(),
                    event.getRemark() != null ? event.getRemark() : "-"
            );

            // sending the email...
            emailService.sendSimpleEmail(event.getUserEmail(), subject, body);

            // publish a success event:
            transactionSuccessProducer.publishSuccess(event.getEventId(), event.getTransactionReference());

        } catch (Exception e) {
            log.error("Failed to process transaction event {}",
                    event.getTransactionReference(), e);
            throw e;  // IMPORTANT: rethrow to trigger retry ...
        }
    }
}
