package net.engineerAnsh.BankApplication.Kafka.Consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Email.EmailServiceImpl;
import net.engineerAnsh.BankApplication.Kafka.Event.AccountNotificationEvent;
import net.engineerAnsh.BankApplication.Email.EmailTemplateService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountEventConsumer {

    private final EmailServiceImpl emailService;
    private final EmailTemplateService emailTemplateService;

    private String buildSubject(AccountNotificationEvent event) {
        return switch (event.getEventType()) {
            case ACCOUNT_CREATED -> "Account Created Successfully";
            case ACCOUNT_ACTIVATED -> "Account Activated";
            case ACCOUNT_BLOCKED -> "Account Blocked";
            case ACCOUNT_FROZEN -> "Account Frozen";
            case ACCOUNT_CLOSED -> "Account Closed";
        };
    }

    @KafkaListener(topics = "account-events",
            groupId = "account-notification-group"
    )
    public void consumeAccountEvent(AccountNotificationEvent event) {
        log.info("Received account event: {}", event);
        String body = emailTemplateService.buildAccountEmailBody(event); // It will build the message according to accountEvent type...
        emailService.sendHtmlEmail(
                event.getEmail(),
                buildSubject(event), // build subject according to accountEvent type...
                body
        );
    }

}
