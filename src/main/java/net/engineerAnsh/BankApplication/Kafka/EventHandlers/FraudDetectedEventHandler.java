package net.engineerAnsh.BankApplication.Kafka.EventHandlers;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Email.EmailServiceImpl;
import net.engineerAnsh.BankApplication.Email.EmailTemplateService;
import net.engineerAnsh.BankApplication.Fraud.FraudDecision;
import net.engineerAnsh.BankApplication.Kafka.Builder.EventProcessedBuilder;
import net.engineerAnsh.BankApplication.Kafka.Event.EventProcessedEvent;
import net.engineerAnsh.BankApplication.Kafka.Event.FraudDetectedEvent;
import net.engineerAnsh.BankApplication.Kafka.Event.TransactionEvent;
import net.engineerAnsh.BankApplication.Kafka.Producer.TransactionEventProducer;
import net.engineerAnsh.BankApplication.Services.AccountService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectedEventHandler implements TransactionEventHandler<FraudDetectedEvent> {

    private final AccountService accountService;
    private final EmailServiceImpl emailService;
    private final EmailTemplateService emailTemplateService;
    private final EventProcessedBuilder eventProcessedBuilder;
    private final TransactionEventProducer transactionEventProducer;


    private String getEventType(TransactionEvent event) {
        JsonTypeName annotation = event.getClass().getAnnotation(JsonTypeName.class);
        return annotation != null ? annotation.value() : "UNKNOWN";
    }

    private String buildSubject(FraudDetectedEvent event) {
        return switch (event.getDecision()) {
            case SUSPICIOUS -> "⚠️ Suspicious Activity Detected";
            case BLOCK -> "⚠️ Transaction Blocked";
            case FREEZE_ACCOUNT -> "🚨 Account Frozen Due to Suspicious Activity";
            case SAFE -> "Transaction is safe";
        };
    }

    private void freezeAccountDueToFraud(FraudDetectedEvent event) {
        try {
            accountService.freezeAccountsForFrauds(event.getAccountNumber());
            log.error("Account: {} frozen, reason: {}", event.getAccountNumber(), event.getReason());
        } catch (Exception ex) {
            log.error("Failed to freeze account, because: {}", ex.getMessage());
        }
    }

    private void handleFraud(FraudDetectedEvent event) {
        String subject = buildSubject(event);
        String body = emailTemplateService.buildFraudDetectedEmailBody(event);
        if (event.getDecision() == FraudDecision.FREEZE_ACCOUNT) freezeAccountDueToFraud(event);
        emailService.sendHtmlEmail(event.getEmail(), subject, body);
    }

    @Override
    public void handle(FraudDetectedEvent event) {
        log.warn("🚨 Fraud event received: {}", event);
        handleFraud(event);
        log.info("Fraud event processed: {}", event.getEventId());

        // Publishing a success event:
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
