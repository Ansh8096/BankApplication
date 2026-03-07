package net.engineerAnsh.BankApplication.Kafka.Schedular;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Entity.OutboxEvent;
import net.engineerAnsh.BankApplication.Enum.OutboxStatus;
import net.engineerAnsh.BankApplication.Kafka.Event.TransactionCompletedEvent;
import net.engineerAnsh.BankApplication.Kafka.Event.UserLoginEvent;
import net.engineerAnsh.BankApplication.Kafka.Event.UserRegisteredEvent;
import net.engineerAnsh.BankApplication.Kafka.Producer.TransactionEventProducer;
import net.engineerAnsh.BankApplication.Kafka.Producer.UserLoginEventProducer;
import net.engineerAnsh.BankApplication.Kafka.Producer.UserRegisteredEventProducer;
import net.engineerAnsh.BankApplication.Repository.OutboxEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventProcessor {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final UserLoginEventProducer loginEventProducer;
    private final UserRegisteredEventProducer registeredEventProducer;
    private final TransactionEventProducer transactionEventProducer;
    private static final int MAX_RETRIES = 5;

    @Scheduled(fixedDelayString = "${outbox.poll.interval:5000}")
    @Transactional
    public void processOutboxEvents() {

        List<OutboxEvent> events =
                outboxEventRepository.findPendingEventsForUpdate(
                        OutboxStatus.PENDING,
                        PageRequest.of(0, 10)
                );

        if (events.isEmpty()) {
            return;
        }

        for (OutboxEvent event : events) {
            log.info("Publishing the kafka event for: {}", event.getEventType());
            try {

                event.setStatus(OutboxStatus.PROCESSING);
                outboxEventRepository.save(event);

                // publishing the kafka event...
                publishEvent(event);

                log.info("Successfully published the kafka event for: {}", event.getEventType());

                event.setStatus(OutboxStatus.PROCESSED);
                event.setProcessedAt(LocalDateTime.now());
                event.setLastError(null);

            } catch (Exception ex) {

                log.error("Failed to publish event: {}", event.getEventType(), ex);

                event.setRetryCount(event.getRetryCount() + 1);
                event.setLastError(ex.getMessage());

                if (event.getRetryCount() >= MAX_RETRIES) {
                    event.setStatus(OutboxStatus.FAILED); // stop retrying
                } else {
                    event.setStatus(OutboxStatus.PENDING);
                }
            }

            outboxEventRepository.save(event);
        }
    }

    private void publishEvent(OutboxEvent event) throws JsonProcessingException {

        switch (event.getEventType()) {
            case USER_REGISTERED:
                UserRegisteredEvent registeredEvent =
                        objectMapper.readValue(
                                event.getPayload(),
                                UserRegisteredEvent.class
                        );
                registeredEventProducer.publishUserRegistrationEventSuccess(registeredEvent);
                break;

            case USER_LOGIN:
                UserLoginEvent loginEvent =
                        objectMapper.readValue(
                                event.getPayload(),
                                UserLoginEvent.class
                        );
                loginEventProducer.publishUserLoginEventSuccess(loginEvent);
                break;

            case TRANSACTION_COMPLETED:
                TransactionCompletedEvent txnEvent =
                        objectMapper.readValue(
                                event.getPayload(),
                                TransactionCompletedEvent.class
                        );
                transactionEventProducer.publishTransactionCompleted(txnEvent);
                break;

            default:
                throw new IllegalArgumentException("Unknown event type: " + event.getEventType());
        }
    }

}

