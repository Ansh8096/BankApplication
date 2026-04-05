package net.engineerAnsh.BankApplication.Kafka.Event;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.engineerAnsh.BankApplication.Enum.transaction.TransactionType;
import net.engineerAnsh.BankApplication.Fraud.FraudDecision;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonTypeName("FRAUD_DETECTED")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FraudDetectedEvent implements TransactionEvent, IdentifiableEvent {

    private String eventId;
    private Long userId;
    private String name;
    private String email;
    private String accountNumber;
    private TransactionType transactionType;
    private String transactionReference;
    private BigDecimal amount;
    private FraudDecision decision; // BLOCK / FREEZE / SUSPICIOUS
    private String reason;
    private LocalDateTime occurredAt;

}