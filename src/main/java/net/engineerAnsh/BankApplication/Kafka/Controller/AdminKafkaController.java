package net.engineerAnsh.BankApplication.Kafka.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Kafka.Entity.FailedKafkaEvent;
import net.engineerAnsh.BankApplication.Kafka.Enums.FailedEventStatus;
import net.engineerAnsh.BankApplication.Kafka.Repository.FailedKafkaEventRepository;
import net.engineerAnsh.BankApplication.Kafka.Service.FailedKafkaEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/admin/kafka")
@RequiredArgsConstructor
public class AdminKafkaController {

    private final FailedKafkaEventService failedKafkaEventService;
    private final FailedKafkaEventRepository failedKafkaEventRepository;

    @GetMapping("/get-failed-events")
    public ResponseEntity<List<FailedKafkaEvent>> getFailedEvents() {
        List<FailedKafkaEvent> events =
                failedKafkaEventRepository.findByStatusNot(FailedEventStatus.RESOLVED);
        return ResponseEntity.ok(events);
    }

    @PostMapping("/retry/{id}")
    public ResponseEntity<String> retryEvent(@PathVariable Long id) {
        failedKafkaEventService.retryFailedEvent(id);
        return ResponseEntity.ok("Event retried successfully");
    }

    @PostMapping("/retry-all")
    public ResponseEntity<String> retryAllEvents() throws JsonProcessingException {
        failedKafkaEventService.retryAllFailedEvents();
        return ResponseEntity.ok("All failed events retried");
    }
}
