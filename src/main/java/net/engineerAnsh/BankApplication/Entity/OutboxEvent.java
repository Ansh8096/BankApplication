package net.engineerAnsh.BankApplication.Entity;

import jakarta.persistence.*;
import lombok.*;
import net.engineerAnsh.BankApplication.Enum.OutboxEventType;
import net.engineerAnsh.BankApplication.Enum.OutboxStatus;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events",
        indexes = {
                @Index(name = "idx_outbox_status", columnList = "status"),
                @Index(name = "idx_outbox_created_at", columnList = "createdAt")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Type of event (USER_REGISTERED, USER_LOGIN etc.)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxEventType eventType;

    // Stores the entire original Kafka message...
    @Lob // @Lob = Large Object (used for Long json)
    @JdbcTypeCode(SqlTypes.LONGVARCHAR) // Tells Hibernate which SQL type to use, 'LONGVARCHAR' id used for 'JSON payloads'...
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload; // JSON payload of the event...

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(nullable = false)
    private int retryCount = 0;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    // Time event was created
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Time event was successfully published...
    private LocalDateTime processedAt;
}