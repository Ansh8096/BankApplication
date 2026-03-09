package net.engineerAnsh.BankApplication.Kafka.Producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Kafka.Event.KycEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KycEventProducer {

    private static final String KYC_TOPIC = "kyc-events";

    private final KafkaTemplate<String, KycEvent> kafkaTemplate;

    public void kycEventPublish(KycEvent kycEvent) {

        kafkaTemplate.send(KYC_TOPIC, String.valueOf(kycEvent.getUserId()), kycEvent)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish KYC event for user {}", kycEvent.getEmail(), ex);
                    } else {
                        log.info("KYC event published successfully for user {}", kycEvent.getEmail());
                    }
                });
    }

}
