package net.engineerAnsh.BankApplication.Kafka.Entity;

import jakarta.persistence.*;
import lombok.*;
import net.engineerAnsh.BankApplication.Kafka.Enums.FailedEventStatus;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Table(name = "failed_kafka_event")
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailedKafkaEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    // Stores the Kafka topic where the event came from...
    @Column(nullable = false)
    private String topic;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    // Stores the entire original Kafka message...
    @Lob // @Lob = Large Object (used for Long json)
    @JdbcTypeCode(SqlTypes.LONGVARCHAR) // Tells Hibernate which SQL type to use, 'LONGVARCHAR' id used for 'JSON payloads'...
    @Column(nullable = false)
    private String payload;

    // It stores the reason for failure...
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FailedEventStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_retried_at")
    private LocalDateTime lastRetriedAt;
}
