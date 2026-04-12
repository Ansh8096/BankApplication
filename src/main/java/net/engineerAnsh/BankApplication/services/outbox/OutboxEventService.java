package net.engineerAnsh.BankApplication.services.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Entity.OutboxEvent;
import net.engineerAnsh.BankApplication.Enum.outbox.OutboxEventType;
import net.engineerAnsh.BankApplication.Enum.outbox.OutboxStatus;
import net.engineerAnsh.BankApplication.Repository.OutboxEventRepository;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxEventService {

    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;

    public OutboxEvent buildOutboxEvent(Object event, OutboxEventType eventType)
            throws JsonProcessingException {
        String payload = objectMapper.writeValueAsString(event);
        return OutboxEvent.builder()
                .eventType(eventType)
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();
    }

    public void publishOutBoxEvent(OutboxEvent outboxEvent){
        outboxEventRepository.save(outboxEvent);
    }
}
