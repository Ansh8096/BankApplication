package net.engineerAnsh.BankApplication.Kafka.Event;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@JsonTypeName("EVENT_PROCESSED")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventProcessedEvent implements TransactionEvent, IdentifiableEvent {

    private String eventId;        // original eventId
    private String eventType;      // FRAUD_DETECTED / TRANSACTION_COMPLETED
    private LocalDateTime processedAt;

}
