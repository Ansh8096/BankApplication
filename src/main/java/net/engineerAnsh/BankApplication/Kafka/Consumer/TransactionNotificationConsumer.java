package net.engineerAnsh.BankApplication.Kafka.Consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Email.EmailServiceimpl;
import net.engineerAnsh.BankApplication.Kafka.Event.TransactionCompletedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;


@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionNotificationConsumer {

    private final EmailServiceimpl emailService;

    @KafkaListener(
            topics = "transaction-completed",
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
            emailService.sendSimpleEmail(event.getUserEmail(),subject,body);

        } catch (Exception e) {
            log.error("Failed to process transaction event {}",
                    event.getTransactionReference(), e);
        }
    }
}
