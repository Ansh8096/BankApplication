package net.engineerAnsh.BankApplication.Kafka.Builder;

import net.engineerAnsh.BankApplication.Kafka.Event.EventProcessedEvent;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class EventProcessedBuilder {

    public EventProcessedEvent buildProcessedEvent(String eventId, String eventType){
        return new EventProcessedEvent(
                eventId,
                eventType,
                LocalDateTime.now()
        );
    }

}
