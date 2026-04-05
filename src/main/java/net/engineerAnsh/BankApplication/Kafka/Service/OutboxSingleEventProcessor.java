package net.engineerAnsh.BankApplication.Kafka.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Entity.OutboxEvent;
import net.engineerAnsh.BankApplication.Enum.outbox.OutboxStatus;
import net.engineerAnsh.BankApplication.Kafka.Event.*;
import net.engineerAnsh.BankApplication.Kafka.Producer.AccountEventProducer;
import net.engineerAnsh.BankApplication.Kafka.Producer.KycEventProducer;
import net.engineerAnsh.BankApplication.Kafka.Producer.TransactionEventProducer;
import net.engineerAnsh.BankApplication.Kafka.Producer.UserEventProducer;
import net.engineerAnsh.BankApplication.Repository.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxSingleEventProcessor {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final TransactionEventProducer transactionEventProducer;
    private final KycEventProducer kycEventProducer;
    private final AccountEventProducer accountEventProducer;
    private final UserEventProducer userEventProducer;

    @Value("${outbox.max-retries}")
    private int MAX_RETRIES;

    @Transactional
    public List<OutboxEvent> fetchEventsForProcessing(){
        return outboxEventRepository.findPendingEventsForUpdate(
                List.of(
                        OutboxStatus.PENDING,
                        OutboxStatus.RETRIED
                ),
                PageRequest.of(0, 10)
        );
    }

    private void publishEvent(OutboxEvent event) throws JsonProcessingException {
        switch (event.getEventType()) {
            case USER_REGISTERED:
                UserRegisteredEvent registeredEvent =
                        objectMapper.readValue(
                                event.getPayload(),
                                UserRegisteredEvent.class
                        );
                userEventProducer.publishUserRegisteredEvent(registeredEvent);
                break;

            case USER_LOGIN:
                UserLoginEvent loginEvent =
                        objectMapper.readValue(
                                event.getPayload(),
                                UserLoginEvent.class
                        );
                userEventProducer.publishUserLoginEvent(loginEvent);
                break;

            case TRANSACTION_COMPLETED:
                TransactionCompletedEvent txnEvent =
                        objectMapper.readValue(
                                event.getPayload(),
                                TransactionCompletedEvent.class
                        );
                transactionEventProducer.publishTxnEvent(txnEvent);
                break;

            case KYC_EVENT:
                KycEvent kycEvent =
                        objectMapper.readValue(
                                event.getPayload(),
                                KycEvent.class
                        );
                kycEventProducer.kycEventPublish(kycEvent);
                break;

            case ACCOUNT_NOTIFICATION:
                AccountNotificationEvent accountEvent =
                        objectMapper.readValue(
                                event.getPayload(),
                                AccountNotificationEvent.class
                        );
                accountEventProducer.accountEventPublish(accountEvent);
                break;

            case FRAUD_DETECTED:
                FraudDetectedEvent fraudDetectedEvent =
                        objectMapper.readValue(event.getPayload(),
                                FraudDetectedEvent.class
                        );
                transactionEventProducer.publishTxnEvent(fraudDetectedEvent);
                break;

            default:
                throw new IllegalArgumentException("Unknown event type: " + event.getEventType());
        }
    }

    @Transactional
    public void processSingleEvent(OutboxEvent event) {

        log.info("Publishing kafka event for: {}", event.getEventType());

        try {
            // mark as processing
            event.setStatus(OutboxStatus.PROCESSING);
            outboxEventRepository.save(event);

            // publish event
            publishEvent(event);

            log.info("Successfully published event: {}", event.getEventType());

            // mark success
            event.setStatus(OutboxStatus.PROCESSED);
            event.setProcessedAt(LocalDateTime.now());
            event.setLastError(null);

        } catch (Exception ex) {

            log.error("Failed to publish event: {}", event.getEventType(), ex);

            event.setRetryCount(event.getRetryCount() + 1);
            event.setLastError(ex.getMessage());
            event.setLastRetriedAt(LocalDateTime.now());

            if (event.getRetryCount() >= MAX_RETRIES) {
                event.setStatus(OutboxStatus.FAILED); // or PERMANENTLY_FAILED
            } else {
                event.setStatus(OutboxStatus.RETRIED);
            }
        }

        outboxEventRepository.save(event);
    }

}