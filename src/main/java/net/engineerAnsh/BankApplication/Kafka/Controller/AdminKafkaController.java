package net.engineerAnsh.BankApplication.Kafka.Controller;

import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Kafka.Dto.FailedKafkaEventResponse;
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

    @GetMapping("/get-failed-events")
    public ResponseEntity<List<FailedKafkaEventResponse>> getFailedEvents() {
        List<FailedKafkaEventResponse> failedKafkaEvents =
                failedKafkaEventService.findPendingFailedKafkaEvents();
        return ResponseEntity.ok(failedKafkaEvents);
    }

    @PostMapping("/retry")
    public ResponseEntity<String> retryEvent(@RequestParam String eventId) throws Exception {
        failedKafkaEventService.retryFailedEventByEventId(eventId);
        return ResponseEntity.ok("Event retried successfully");
    }

    @PostMapping("/retry-all")
    public ResponseEntity<String> retryAllEvents() {
        failedKafkaEventService.retryAllFailedEvents();
        return ResponseEntity.ok("All failed events retried");
    }
}
