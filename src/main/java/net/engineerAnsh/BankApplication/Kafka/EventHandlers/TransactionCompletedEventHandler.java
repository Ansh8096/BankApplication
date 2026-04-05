package net.engineerAnsh.BankApplication.Kafka.EventHandlers;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Email.EmailServiceImpl;
import net.engineerAnsh.BankApplication.Email.EmailTemplateService;
import net.engineerAnsh.BankApplication.Kafka.Builder.EventProcessedBuilder;
import net.engineerAnsh.BankApplication.Kafka.Event.TransactionCompletedEvent;
import net.engineerAnsh.BankApplication.Kafka.Event.TransactionEvent;
import net.engineerAnsh.BankApplication.Kafka.Event.EventProcessedEvent;
import net.engineerAnsh.BankApplication.Kafka.Producer.TransactionEventProducer;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionCompletedEventHandler implements TransactionEventHandler<TransactionCompletedEvent> {

    private final EmailServiceImpl emailService;
    private final EmailTemplateService emailTemplateService;
    private final TransactionEventProducer transactionEventProducer;
    private final EventProcessedBuilder eventProcessedBuilder;


    private void txnAlert(TransactionCompletedEvent event) {

        String subject = "Transaction Alert - " + event.getType();
        String body = emailTemplateService.buildTxnEmailBody(event);

        // sending the email...
        emailService.sendHtmlEmail(event.getUserEmail(), subject, body);
    }

    private String getEventType(TransactionEvent event) {
        JsonTypeName annotation = event.getClass().getAnnotation(JsonTypeName.class);
        return annotation != null ? annotation.value() : "UNKNOWN";
    }

    @Override
    public void handle(TransactionCompletedEvent event) {
        log.info("Received transaction event: {}", event.getTransactionReference());

        // notify the user...
        txnAlert(event);

        // Publish a success event:
        // We are using try catch here, because we don't want to trigger a retry because of this...
        try {
            EventProcessedEvent eventProcessedEvent = eventProcessedBuilder.buildProcessedEvent(event.getEventId(), getEventType(event));
            transactionEventProducer.publishTxnEvent(eventProcessedEvent);
            log.info("Successfully published an eventProcessedEvent: {}", event);

        } catch (Exception e) {
            log.error("Failed to publish an eventProcessedEvent: {}", event);

        }

    }
}
