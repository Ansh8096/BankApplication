package net.engineerAnsh.BankApplication.Kafka.Event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.engineerAnsh.BankApplication.Kafka.Enums.KycEventType;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KycEvent {

    private Long userId;

    private String email;

    private KycEventType eventType;

    private String documentType;

    private String reason;

}