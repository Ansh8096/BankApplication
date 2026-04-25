package net.engineerAnsh.BankApplication.Kafka.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Admin Kafka APIs", description = "Operations for managing failed Kafka events and retries")
public class AdminKafkaController {

    private final FailedKafkaEventService failedKafkaEventService;

    @Operation(summary = "Get failed Kafka events", description = "Fetch all pending failed Kafka events")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Failed events fetched successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/get-failed-events")
    public ResponseEntity<List<FailedKafkaEventResponse>> getFailedEvents() {
        List<FailedKafkaEventResponse> failedKafkaEvents =
                failedKafkaEventService.findPendingFailedKafkaEvents();
        return ResponseEntity.ok(failedKafkaEvents);
    }

    @Operation(summary = "Retry failed event", description = "Retry a specific failed Kafka event using event ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event retried successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid event ID"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PostMapping("/retry")
    public ResponseEntity<String> retryEvent(@RequestParam String eventId) throws Exception {
        failedKafkaEventService.retryFailedEventByEventId(eventId);
        return ResponseEntity.ok("Event retried successfully");
    }

    @Operation(summary = "Retry all failed events", description = "Retry all pending failed Kafka events")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All events retried successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PostMapping("/retry-all")
    public ResponseEntity<String> retryAllEvents() {
        failedKafkaEventService.retryAllFailedEvents();
        return ResponseEntity.ok("All failed events retried");
    }
}
