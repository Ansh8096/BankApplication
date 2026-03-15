package net.engineerAnsh.BankApplication.Kafka.Producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Kafka.Event.AccountNotificationEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountEventProducer {

    private static final String KYC_TOPIC = "account-events";

    private final KafkaTemplate<String, AccountNotificationEvent> kafkaTemplate;

    public void accountEventPublish(AccountNotificationEvent accountEvent) {

        kafkaTemplate.send(KYC_TOPIC, accountEvent.getEventId(), accountEvent)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish an account event of user {} for: {}",
                                accountEvent.getEmail(), accountEvent.getEventType(), ex);
                    } else {
                        log.info("Account event is published successfully of user {} for: {}",
                                accountEvent.getEmail(), accountEvent.getEventType());
                    }
                });
    }

}
