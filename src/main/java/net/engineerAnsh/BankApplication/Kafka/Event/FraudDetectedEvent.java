package net.engineerAnsh.BankApplication.Kafka.Event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.engineerAnsh.BankApplication.Enum.TransactionType;
import net.engineerAnsh.BankApplication.Fraud.FraudDecision;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FraudDetectedEvent {

    private String eventId;
    private Long userId;
    private String email;
    private String accountNumber;
    private TransactionType transactionType;
    private BigDecimal amount;
    private FraudDecision decision; // BLOCK / FREEZE / SUSPICIOUS
    private String reason;
    private LocalDateTime timestamp;
}