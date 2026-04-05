package net.engineerAnsh.BankApplication.Kafka.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.engineerAnsh.BankApplication.Kafka.Enums.FailedEventStatus;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FailedKafkaEventResponse {

    private String eventId;
    private String topic;
    private String eventType;
    private FailedEventStatus status;
    private String errorMessage;
    private int retryCount;

}
