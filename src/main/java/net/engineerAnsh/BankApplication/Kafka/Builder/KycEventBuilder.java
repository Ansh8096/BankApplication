package net.engineerAnsh.BankApplication.Kafka.Builder;

import net.engineerAnsh.BankApplication.Entity.User;
import net.engineerAnsh.BankApplication.Kafka.Enums.KycEventType;
import net.engineerAnsh.BankApplication.Kafka.Event.KycEvent;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class KycEventBuilder {

    public KycEvent buildKycEvent(User user, KycEventType kycEventType, String reason, String documentType, String referenceId) {

        return new KycEvent(
                user.getUserId(),
                referenceId,
                user.getName(),
                user.getEmail(),
                kycEventType,
                documentType,
                reason,
                LocalDateTime.now()
        );
    }
}
