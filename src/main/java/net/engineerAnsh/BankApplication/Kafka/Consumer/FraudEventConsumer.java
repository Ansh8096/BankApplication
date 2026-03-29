package net.engineerAnsh.BankApplication.Kafka.Consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Email.EmailServiceImpl;
import net.engineerAnsh.BankApplication.Kafka.Event.FraudDetectedEvent;
import net.engineerAnsh.BankApplication.Services.AccountService;
import net.engineerAnsh.BankApplication.Email.EmailTemplateService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudEventConsumer {

    private final AccountService accountService;
    private final EmailServiceImpl emailService;
    private final EmailTemplateService emailTemplateService;


    private String buildSubject(FraudDetectedEvent event) {
        return switch (event.getDecision()) {
            case SUSPICIOUS -> "⚠️ Suspicious Activity Detected";
            case BLOCK -> "⚠️ Transaction Blocked";
            case FREEZE_ACCOUNT -> "🚨 Account Frozen Due to Suspicious Activity";
            case SAFE -> "Transaction is safe";
        };
    }

    private void freezeAccountAndNotify(FraudDetectedEvent event, String subject, String body) {
        try {
            accountService.freezeAccountsForFrauds(event.getAccountNumber());
            log.error("Account: {} frozen, reason: {}", event.getAccountNumber(),event.getReason());
            emailService.sendSimpleEmail(event.getEmail(), subject, body);
        } catch (Exception ex) {
            log.error("Failed to freeze account, because: {}", ex.getMessage());
        }
    }

    private void handleFraud(FraudDetectedEvent event) {

        String body = emailTemplateService.buildFraudDetectedEmailBody(event);
        String subject = buildSubject(event);

        switch (event.getDecision()) {
            case FREEZE_ACCOUNT -> freezeAccountAndNotify(event, subject, body);
            case BLOCK -> {
                log.error("Blocked transaction: {}", event.getReason());
                emailService.sendSimpleEmail(event.getEmail(), subject, body);
            }
            case SUSPICIOUS -> {
                log.warn("Suspicious activity: {}", event.getReason());
                emailService.sendSimpleEmail(event.getEmail(), subject, body);
            }
        }
    }

    @KafkaListener(
            topics = "fraud-events",
            groupId = "fraud-consumer-group"
    )
    public void consume(FraudDetectedEvent event) {
        try {
            log.warn("🚨 Fraud event received: {}", event);
            handleFraud(event);

        } catch (Exception e) {
            log.error("Failed to process fraud event", e);
        }
    }



}